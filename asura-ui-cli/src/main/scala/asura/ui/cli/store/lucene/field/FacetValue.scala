package asura.ui.cli.store.lucene.field

import org.apache.lucene.document.{Document, Field => LuceneField}
import org.apache.lucene.facet.{FacetField => LuceneFacetField}

case class FacetValue(field: FacetField, path: String*) {

  lazy val pathString = path.mkString("/")

  def write(document: Document): Unit = {
    val updatedPath = if (field.hierarchical) path.toList ::: List("$ROOT$") else path
    document.add(new LuceneField(field.name, pathString, FieldType.STORED.lucene()))
    document.add(new LuceneFacetField(field.name, updatedPath: _*))
  }

  override def toString: String = s"FacetValue(field: $field, path: $pathString)"

}
