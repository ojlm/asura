package asura.core.model

case class QueryProject(var group: String, id: String, text: String) extends QueryPage {
  var includeGroup = false
}
