package asura.ui.ocr

import com.fasterxml.jackson.annotation.JsonIgnore

case class Word(
                 text: String,
                 x: Int,
                 y: Int,
                 width: Int,
                 height: Int,
                 confidence: Float,
               ) {
  @JsonIgnore var prev: Word = null
  @JsonIgnore var next: Word = null
}
