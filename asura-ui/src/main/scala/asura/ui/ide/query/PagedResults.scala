package asura.ui.ide.query

case class PagedResults[T](
                            total: Long,
                            list: Seq[T],
                            sorts: Array[Any] = null,
                          )
