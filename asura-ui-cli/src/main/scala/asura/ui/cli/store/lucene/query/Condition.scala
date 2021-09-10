package asura.ui.cli.store.lucene.query

import org.apache.lucene.search.BooleanClause

abstract class Condition(val occur: BooleanClause.Occur)

object Condition {

  case object MUST extends Condition(BooleanClause.Occur.MUST)

  case object FILTER extends Condition(BooleanClause.Occur.FILTER)

  case object SHOULD extends Condition(BooleanClause.Occur.SHOULD)

  case object MUST_NOT extends Condition(BooleanClause.Occur.MUST_NOT)

}
