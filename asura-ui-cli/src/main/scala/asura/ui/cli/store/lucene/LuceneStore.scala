package asura.ui.cli.store.lucene

import java.nio.file.{Files, Path, Paths}

import asura.ui.cli.store.lucene.field.Field
import asura.ui.cli.store.lucene.query.SearchTerm
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager.SearcherAndTaxonomy
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter
import org.apache.lucene.facet.taxonomy.writercache.TaxonomyWriterCache
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{DirectoryReader, IndexReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.SearcherFactory
import org.apache.lucene.store.{MMapDirectory, NIOFSDirectory}

class LuceneStore(
                   val directory: Option[Path] = None,
                   val fullTextSearchable: Boolean = true,
                   val autoCommit: Boolean = false,
                 ) extends Lucene {

  lazy val indexPath = directory.map(_.resolve("index"))
  lazy val taxonomyPath = directory.map(_.resolve("taxonomy"))
  lazy val indexDirectory = indexPath.map(new NIOFSDirectory(_)).getOrElse(new MMapDirectory(Files.createTempDirectory("lucene-index-")))
  lazy val taxonomyDirectory = taxonomyPath.map(new NIOFSDirectory(_)).getOrElse(new MMapDirectory(Files.createTempDirectory("lucene-taxonomy-")))
  lazy val indexWriterConfig = new IndexWriterConfig(analyzer).setOpenMode(OpenMode.CREATE_OR_APPEND)

  override lazy val analyzer: Analyzer = new SmartChineseAnalyzer()
  override lazy val indexWriter: IndexWriter = new IndexWriter(indexDirectory, indexWriterConfig)
  override lazy val indexReader: IndexReader = DirectoryReader.open(indexWriter)
  override lazy val facetsConfig: FacetsConfig = new FacetsConfig()

  lazy val taxonomyWriterCache: TaxonomyWriterCache = DirectoryTaxonomyWriter.defaultTaxonomyWriterCache()
  override lazy val taxonomyWriter: DirectoryTaxonomyWriter = new DirectoryTaxonomyWriter(
    taxonomyDirectory, IndexWriterConfig.OpenMode.CREATE_OR_APPEND, taxonomyWriterCache
  )
  lazy val searcherTaxonomyManager = new SearcherTaxonomyManager(
    indexWriter,
    new SearcherFactory,
    taxonomyWriter
  )

  override def id: Field[Long] = define.field[Long]("_id")

  override def fullText: Field[String] = define.field[String]("_text", sortable = false)

  override def withSearcherAndTaxonomy[R](f: SearcherAndTaxonomy => R): R = {
    searcherTaxonomyManager.maybeRefreshBlocking()
    val instance = searcherTaxonomyManager.acquire()
    try {
      f(instance)
    } finally {
      searcherTaxonomyManager.release(instance)
    }
  }

  override def indexed(builders: Seq[DocumentBuilder]): Unit = {
    if (autoCommit) this.commit()
  }

  override def delete(term: SearchTerm): Unit = {
    indexWriter.deleteDocuments(term.toLucene(this))
    if (autoCommit) this.commit()
  }

  override def deleteAll(commit: Boolean): Unit = {
    indexWriter.deleteAll()
    if (commit) this.commit()
  }

  override def commit(): Unit = {
    indexWriter.commit()
    taxonomyWriter.commit()
    searcherTaxonomyManager.maybeRefresh()
  }

  override def dispose(): Unit = {
    withSearcherAndTaxonomy { instance =>
      instance.searcher.getIndexReader.close()
    }
    indexWriter.close()
    taxonomyWriter.close()
    indexDirectory.close()
    taxonomyDirectory.close()
  }

}

object LuceneStore {

  def apply(path: String): LuceneStore = {
    new LuceneStore(directory = Some(Paths.get(path)))
  }

  def apply(path: Path): LuceneStore = {
    new LuceneStore(directory = Some(path))
  }

}
