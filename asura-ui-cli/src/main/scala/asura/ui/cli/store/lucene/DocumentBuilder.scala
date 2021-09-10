package asura.ui.cli.store.lucene

import scala.collection.mutable.ListBuffer

import asura.ui.cli.store.lucene.field.{FacetValue, Field, FieldValue}
import asura.ui.cli.store.lucene.query.SearchTerm
import org.apache.lucene.document.Document

case class DocumentBuilder(
                            lucene: Lucene,
                            update: Option[SearchTerm] = None,
                            document: Document = new Document(),
                          ) {

  var fullText: List[String] = List.empty[String]

  private var unwrittenFacets = Set.empty[FacetValue]
  private val _values = ListBuffer.empty[FieldValue[_]]

  def values: List[FieldValue[_]] = _values.toList

  def valueForName(name: String): Option[FieldValue[_]] = values.find(_.field.name == name)

  def rebuildFacetsFromDocument(): Unit = {
    lucene.facets.foreach(ff => {
      document.getFields(ff.name).foreach(field => {
        val v = field.stringValue()
        val path = if (ff.hierarchical) v.split("/").toList else List(v)
        unwrittenFacets += ff(path: _*)
      })
    })
  }

  def fields(values: FieldValue[_]*): DocumentBuilder = synchronized {
    values.foreach(v => {
      v.write(document)
      if (v.field.fullTextSearchable) {
        fullText = v.value.toString :: fullText
      }
      _values += v
    })
    this
  }

  def clear(name: String): DocumentBuilder = {
    document.removeFields(name)
    unwrittenFacets = unwrittenFacets.filterNot(_.field.name == name)
    this
  }

  def clear[T](field: Field[T]): DocumentBuilder = {
    clear(field.name)
    clear(field.filterName)
    clear(field.sortName)
  }

  def facets(values: FacetValue*): DocumentBuilder = synchronized {
    unwrittenFacets ++= values
    this
  }

  def remove[T](facetValues: FacetValue*): DocumentBuilder = {
    val field = facetValues.head.field
    val excludePaths = facetValues.map(_.pathString).toSet
    val values = document.getFields(field.name).toList.map(_.stringValue()).distinct
    val updated = values.collect {
      case v if !excludePaths.contains(v) => {
        val path = if (field.hierarchical) v.split("/").toList else List(v)
        new FacetValue(field, path: _*)
      }
    }
    clear(field.name)
    facets(updated: _*)
  }

  def prepareForWriting(): Unit = {
    unwrittenFacets.foreach(v => {
      try {
        if (v.pathString.trim.nonEmpty || v.field.hierarchical) {
          v.write(document)
        }
      } catch {
        case t: Throwable => throw new RuntimeException(s"Fail to write facet: $v", t)
      }
    })
    unwrittenFacets = Set.empty
  }

  def index(): Unit = lucene.index(this)

}
