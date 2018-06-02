package asura.core.job

import asura.core.es.model.JobTrigger

class JobListItem(
                   var _id: String,
                   val summary: String,
                   val description: String,
                   val name: String,
                   val group: String,
                   val scheduler: String,
                   val classAlias: String,
                   val trigger: Seq[JobTrigger],
                   val jobData: Map[String, Any],
                   var state: String,
                   var creator: String = null,
                   var createdAt: String = null,
                 )
