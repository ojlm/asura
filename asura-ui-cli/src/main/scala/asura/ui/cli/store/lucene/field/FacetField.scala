package asura.ui.cli.store.lucene.field

case class FacetField(
                       name: String,
                       hierarchical: Boolean,
                       multiValued: Boolean,
                       requireDimCount: Boolean,
                     ) {

  def apply(path: String*): FacetValue = FacetValue(this, path: _*)

  override def toString: String = name

}
