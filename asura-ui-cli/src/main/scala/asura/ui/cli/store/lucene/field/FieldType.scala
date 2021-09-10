package asura.ui.cli.store.lucene.field

import org.apache.lucene.index.{DocValuesType, IndexOptions}

object FieldType {
  val STORED = FieldType(stored = true, frozen = true)
  val UN_TOKENIZED = FieldType(stored = true, tokenized = false, frozen = true)
  val NUMERIC = FieldType(
    stored = true, tokenized = false, frozen = true, docValuesType = DocValuesType.NUMERIC
  )
}

case class FieldType(
                      stored: Boolean = false,
                      tokenized: Boolean = true,
                      storeTermVectors: Boolean = false,
                      storeTermVectorOffsets: Boolean = false,
                      storeTermVectorPositions: Boolean = false,
                      storeTermVectorPayloads: Boolean = false,
                      omitNorms: Boolean = false,
                      indexOptions: IndexOptions = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS,
                      frozen: Boolean = false,
                      docValuesType: DocValuesType = DocValuesType.NONE,
                      dimensionCount: Int = 0,
                      dimensionNumBytes: Int = 0,
                    ) {
  def lucene(): org.apache.lucene.document.FieldType = {
    val ft = new org.apache.lucene.document.FieldType()
    ft.setStored(stored)
    ft.setTokenized(tokenized)
    ft.setStoreTermVectors(storeTermVectors)
    ft.setStoreTermVectorOffsets(storeTermVectorOffsets)
    ft.setStoreTermVectorPositions(storeTermVectorPositions)
    ft.setStoreTermVectorPayloads(storeTermVectorPayloads)
    ft.setOmitNorms(omitNorms)
    ft.setIndexOptions(indexOptions)
    ft.setDocValuesType(docValuesType)
    ft.setDimensions(dimensionCount, dimensionNumBytes)
    if (frozen) ft.freeze()
    ft
  }
}
