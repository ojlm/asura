package asura.ui.cli.server.ide.local

import java.nio.file.Path

import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.cli.store.lucene.{DocumentBuilder, LuceneStore}
import asura.ui.ide.model.AbsDoc

abstract class LocalStore[T <: AbsDoc](path: Path) extends LuceneStore(directory = Some(path), autoCommit = true) {

  val creator = define.field[String]("creator", FieldType.UN_TOKENIZED)
  val createdAt = define.field[Long]("createdAt", FieldType.NUMERIC)
  val updatedAt = define.field[Long]("updatedAt", FieldType.NUMERIC)

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

}
