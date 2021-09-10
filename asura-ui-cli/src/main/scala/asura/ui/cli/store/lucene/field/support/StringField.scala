package asura.ui.cli.store.lucene.field.support

import asura.ui.cli.store.lucene.field.{Field, FieldSupport, FieldValue}
import asura.ui.cli.store.lucene.query.SearchTerm
import asura.ui.cli.store.lucene.query.SearchTerm.{PhraseSearchTerm, TermSearchTerm}
import org.apache.lucene.document.{Document, SortedDocValuesField, Field => LuceneField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.util.{ByteBlockPool, BytesRef}

object StringField extends FieldSupport[String] {

  override def store(field: Field[String], value: String, document: Document): Unit = {
    document.add(new LuceneField(field.name, value, field.fieldType.lucene()))
  }

  override def separateFilter: Boolean = false

  override def filter(field: Field[String], value: String, document: Document): Unit = {}

  override def sorted(field: Field[String], value: String, document: Document): Unit = {
    val bytes = new BytesRef(value)
    if (bytes.length > ByteBlockPool.BYTE_BLOCK_SIZE - 2) {
      throw new RuntimeException(s"Value for field ${field.sortName} is greater than " +
        s"${ByteBlockPool.BYTE_BLOCK_SIZE - 2} bytes. " +
        "This would cause a Lucene error. Reduce field size or set: sortable = false"
      )
    }
    document.add(new SortedDocValuesField(field.sortName, bytes))
  }

  override def sortFieldType: SortField.Type = SortField.Type.STRING

  override def fromLucene(fields: List[IndexableField]): String = {
    fields.headOption.map(_.stringValue()).orNull
  }

  override def searchTerm(fv: FieldValue[String]): SearchTerm = {
    if (fv.field.fieldType.tokenized) {
      PhraseSearchTerm(Some(fv.field), fv.value)
    } else {
      TermSearchTerm(Some(fv.field), fv.value)
    }
  }

}
