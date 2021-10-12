package asura.ui.cli.store.lucene.field

case class Field[T](
                     name: String,
                     fieldType: FieldType,
                     support: FieldSupport[T],
                     fullTextSearchable: Boolean,
                     filterable: Boolean = true,
                     sortable: Boolean = true,
                   ) {

  lazy val filterName: String = if (support.separateFilter) s"${name}Filter" else name
  lazy val sortName: String = s"${name}Sort"

  def apply(value: T): FieldValue[T] = FieldValue[T](this, value)

  override def toString: String = name

}
