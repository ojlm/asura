package asura.routes.model

import asura.app.routes.model.BaseGetList

case class DbDumpFileSearch(
                             val schedName: String,
                             val jobGroup: String,
                             val jobName: String,
                             val jobKey: String
                           ) extends BaseGetList
