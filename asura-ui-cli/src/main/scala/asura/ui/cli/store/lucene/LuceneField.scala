package asura.ui.cli.store.lucene

import asura.ui.cli.store.lucene.field.{FacetField, Field, FieldSupport, FieldType}

class LuceneField(val lucene: Lucene) {

  def field[T](name: String,
               fieldType: FieldType = FieldType.STORED,
               fullTextSearchable: Boolean = lucene.fullTextSearchable,
               filterable: Boolean = true,
               sortable: Boolean = true,
              )(implicit support: FieldSupport[T]): Field[T] = {
    val field = Field[T](name, fieldType, support, fullTextSearchable, filterable, sortable)
    this.synchronized {
      lucene._fields += field
    }
    field
  }

  def facet(name: String,
            hierarchical: Boolean = false,
            multiValued: Boolean = false,
            requireDimCount: Boolean = false,
           ): FacetField = {
    lucene.facetsConfig.setHierarchical(name, hierarchical)
    lucene.facetsConfig.setMultiValued(name, multiValued)
    lucene.facetsConfig.setRequireDimCount(name, requireDimCount)
    val field = FacetField(name, hierarchical, multiValued, requireDimCount)
    this.synchronized {
      lucene._facets += field
    }
    field
  }

}
