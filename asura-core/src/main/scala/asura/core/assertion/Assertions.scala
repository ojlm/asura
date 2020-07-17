package asura.core.assertion

import scala.collection.mutable

object Assertions {

  // comparison
  val EQ = "$eq"
  val NE = "$ne"
  val GT = "$gt"
  val GTE = "$gte"
  val LT = "$lt"
  val LTE = "$lte"
  val IN = "$in"
  val NIN = "$nin"
  val REGEX = "$regex"
  val IS_NULL = "$is-null"
  val IS_EMPTY = "$is-empty"
  // element
  val TYPE = "$type"
  // array
  val SIZE = "$size"
  // logical
  val AND = "$and"
  val NOT = "$not"
  val NOR = "$nor"
  val OR = "$or"
  // script
  val SCRIPT = "$script"
  // other
  val LIST_AND = "$list-and" // alias of $and, for front end usage
  val LIST_OR = "$list-or" // alias of $or, for front end usage

  private val assertions = mutable.HashMap[String, Assertion]()

  // simple have explicit expect and actual value
  val normals = Seq(
    Eq(), Ne(), Gt(), Gte(), Lt(), Lte(), In(), Nin(), IsNull(), IsEmpty(), Regex(), Size(), Type(),
  )
  // logic or complex computation
  val specials = Seq(
    And(), Nor(), Not(), Or(), Script(), ListAnd(), ListOr()
  )
  normals.foreach(register(_))
  specials.foreach(register(_))

  /** this is not thread safe */
  def register(assertion: Assertion): Unit = {
    assertions += (assertion.name -> assertion)
  }

  def get(name: String): Option[Assertion] = assertions.get(name)

  def getAll() = normals
}
