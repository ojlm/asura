package asura.ui.cli.store.lucene.field

import org.apache.lucene.document.Document

case class FieldValue[T](field: Field[T], value: T) {
  def write(document: Document): Unit = {
    field.support.store(field, value, document)
    if (field.filterable) {
      field.support.filter(field, value, document)
    }
    if (field.sortable) {
      field.support.sorted(field, value, document)
    }
  }
}
