package asura.ui.ocr

import scala.collection.mutable.ArrayBuffer

case class Words(full: String, words: Seq[Word]) {

  def slideSearch(tokens: StringBuilder, chars: String, start: Int): (Boolean, Int) = {
    var found = true
    var pos = start
    chars.foreach(c => {
      if (tokens.length > pos) {
        if (tokens(pos) != c) {
          found = false
        } else {
          pos = pos + 1
        }
      } else {
        found = false
      }
    })
    (found, pos)
  }

  def find(text: String): FindResult = {
    val list = ArrayBuffer[Word]()
    val tokens = new StringBuilder(text.length)
    text.filter(c => !c.isWhitespace) foreach (c => tokens.append(c))
    words.foreach(word => {
      var found = false
      var pos = 0
      var current = word
      var prev: Word = null
      do {
        val tuple = slideSearch(tokens, current.text, pos)
        found = tuple._1
        pos = tuple._2
        prev = current
        current = current.next
      } while (found && pos < tokens.length && current != null)
      if (found && pos == tokens.length) {
        val first = word
        val last = prev
        val x = first.x
        val y = first.y
        val width = last.x + last.width - first.x
        val height = Math.max(first.height, last.height)
        list += Word(text, x, y, width, height, confidence = Math.min(first.confidence, last.confidence))
      }
    })
    FindResult(this, list.toSeq)
  }

}
