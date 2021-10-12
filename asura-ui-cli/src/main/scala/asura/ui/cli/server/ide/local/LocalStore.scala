package asura.ui.cli.server.ide.local

import java.nio.file.Path

import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.cli.store.lucene.{DocumentBuilder, LuceneStore}
import asura.ui.ide.model.AbsDoc

abstract class LocalStore[T <: AbsDoc](path: Path) extends LuceneStore(directory = Some(path), autoCommit = true) {

  lazy val creator = define.field[String]("creator", FieldType.UN_TOKENIZED)
  lazy val createdAt = define.field[Long]("createdAt", FieldType.NUMERIC)
  lazy val updatedAt = define.field[Long]("updatedAt", FieldType.NUMERIC)

  val docToModel: SearchResult => T
  val modelToDoc: T => DocumentBuilder

  def fillCommonField(model: T, doc: SearchResult): T = {
    model.id = doc(id)
    model.creator = doc(creator)
    model.createdAt = doc(createdAt)
    model.updatedAt = doc(updatedAt)
    model
  }

  def fillCommonField(builder: DocumentBuilder, model: T): DocumentBuilder = {
    builder.fields(
      creator(model.creator),
      createdAt(model.createdAt),
      updatedAt(model.updatedAt),
    )
  }

  def getByIdSync(id: String): Option[T] = {
    query(docToModel).filter(this.id(id)).limit(1).search().entries.headOption
  }

  def saveSync(id: String, item: T): String = {
    index(id, modelToDoc(item))
  }

  def saveSync(item: T): String = {
    index(modelToDoc(item))
  }

  def deleteSync(id: String): Unit = {
    delete(this.id(id))
  }

}
