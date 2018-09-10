package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.core.es.model.{Job, JobData}
import asura.core.es.{EsClient, EsClientConfig}
import asura.core.job.actor.JobStatusActor.JobQueryMessage
import com.sksamuel.elastic4s.http.ElasticDsl._

class JobServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  test("delete-index") {
    EsClient.esClient.execute {
      deleteIndex(Job.Index)
    }.await match {
      case Right(res) =>
        println(res)
      case _ =>
    }
  }

  test("create-index") {
    val isOk = IndexService.initCheck(Job)
    assertResult(true)(isOk)
  }

  test("index") {
    val job = Job(
      summary = "job",
      description = "hi job",
      name = "job",
      group = "test",
      scheduler = "scheduler",
      classAlias = "aaa",
      trigger = Nil,
      jobData = JobData()
    )
    JobService.index(job).await match {
      case Right(res) =>
        println(res.result)
      case _ =>
    }
  }

  test("query") {
    val q = JobQueryMessage(text = "man")
    JobService.query(q).await match {
      case Left(_) =>
      case Right(res) =>
        println(res)
    }
  }
}
