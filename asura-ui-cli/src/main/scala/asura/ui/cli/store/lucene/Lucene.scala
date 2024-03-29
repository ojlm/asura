package asura.ui.cli.store.lucene

import java.nio.file.Path
import java.util.Collections

import scala.jdk.CollectionConverters._

import asura.ui.cli.store.lucene.field.{FacetField, Field}
import asura.ui.cli.store.lucene.query.SearchTerm.GroupedSearchTerm
import asura.ui.cli.store.lucene.query.{Condition, QueryBuilder, SearchResult, SearchTerm}
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager.SearcherAndTaxonomy
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter
import org.apache.lucene.index.{IndexReader, IndexWriter}
import org.apache.lucene.queries.mlt.MoreLikeThis

/** a lucene index */
trait Lucene {

  private[lucene] var _fields = Set.empty[Field[_]]
  private[lucene] var _facets = Set.empty[FacetField]

  lazy val define: LuceneField = new LuceneField(this)
  lazy val idWorker: IdWorker = new IdWorker(0)

  def nextId(): String = idWorker.nextId().toHexString

  def directory: Option[Path]

  def fullTextSearchable: Boolean

  def analyzer: Analyzer

  def doc(): DocumentBuilder = DocumentBuilder(this)

  def query[T](converter: SearchResult => T): QueryBuilder[T] = QueryBuilder(this, converter = converter)

  def query(): QueryBuilder[SearchResult] = QueryBuilder[SearchResult](this, converter = sr => sr)

  def fields: Set[Field[_]] = _fields

  def field[T](name: String): Field[T] = {
    fields.find(_.name == name).getOrElse(throw new RuntimeException(s"Field $name not found")).asInstanceOf[Field[T]]
  }

  def facets: Set[FacetField] = _facets

  def facet(name: String): FacetField = {
    facets.find(_.name == name).getOrElse(throw new RuntimeException(s"Facet $name not found"))
  }

  def id: Field[String]

  def fullText: Field[String]

  def indexWriter: IndexWriter

  def indexReader: IndexReader

  def facetsConfig: FacetsConfig

  def taxonomyWriter: DirectoryTaxonomyWriter

  def withSearcherAndTaxonomy[R](f: SearcherAndTaxonomy => R): R

  def index(id: String, builder: DocumentBuilder): String = {
    builder.prepareForWriting()
    if (builder.fullText.nonEmpty) {
      builder.fields(fullText(builder.fullText.mkString("\n")))
    }
    builder.fields(this.id(id))
    val doc = facetsConfig.build(taxonomyWriter, builder.document)
    if (builder.update.nonEmpty) {
      delete(builder.update.get)
    }
    indexWriter.addDocuments(Collections.singleton(doc))
    indexed(Seq(builder))
    id
  }

  // return the last id
  def index(builders: DocumentBuilder*): String = {
    var lastDocId: String = null
    builders.foreach(_.prepareForWriting())
    // build docs to insert
    val docs = builders.map(builder => {
      if (builder.fullText.nonEmpty) {
        builder.fields(fullText(builder.fullText.mkString("\n")))
      }
      lastDocId = nextId()
      builder.fields(this.id(lastDocId))
      facetsConfig.build(taxonomyWriter, builder.document)
    })
    // delete existing docs
    val deleteTerms = builders.flatten(_.update)
    if (deleteTerms.nonEmpty) {
      delete(GroupedSearchTerm(1, deleteTerms.map(term => (term, Condition.SHOULD))))
    }
    indexWriter.addDocuments(docs.asJava)
    indexed(builders)
    lastDocId
  }

  def indexed(builders: Seq[DocumentBuilder]): Unit

  def delete(term: SearchTerm): Unit

  def deleteAll(commit: Boolean = true): Unit

  def commit(): Unit

  def dispose(): Unit

  def optimize(): Unit = {
    indexWriter.flush()
    indexWriter.commit()
    indexWriter.forceMergeDeletes()
    indexWriter.deleteUnusedFiles()
  }

  def moreLikeThis: MoreLikeThis = {
    val mlt = new MoreLikeThis(indexReader)
    mlt.setAnalyzer(analyzer)
    mlt
  }

}

object Lucene {

  val specialCharacters: Set[Char] = Set('~', '*', '?', '^', ':', '(', ')', '"', '-', '+', '\'')

  def isLuceneWord(word: String): Boolean = specialCharacters.exists(c => word.contains(c))

  def removeSpecialCharacters(text: String): String = text.filterNot(specialCharacters.contains)

  def queryToWords(query: String): List[String] = {
    query.split(' ').toList.collect {
      case w if !w.equalsIgnoreCase("AND") && !w.equalsIgnoreCase("OR") =>
        val colon = w.indexOf(':')
        val term = if (colon > -1) {
          w.substring(colon + 1)
        } else {
          w
        }
        removeSpecialCharacters(term)
    }
  }

}
