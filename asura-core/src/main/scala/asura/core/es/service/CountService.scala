package asura.core.es.service

import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model.{Activity, FieldKeys, JobReport}
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.Future

object CountService extends CommonService {

  def countIndex(index: String): Future[Long] = {
    EsClient.esClient.execute {
      count(index)
    }.map(res => {
      if (res.isSuccess) res.result.count else 0L
    })
  }

  def countActivity(`type`: String): Future[Long] = {
    EsClient.esClient.execute {
      count(Activity.Index).query(termQuery(FieldKeys.FIELD_TYPE, `type`))
    }.map(res => {
      if (res.isSuccess) res.result.count else 0L
    })
  }

  def countActivities(types: Seq[String]): Future[Long] = {
    EsClient.esClient.execute {
      count(Activity.Index).query(termsQuery(FieldKeys.FIELD_TYPE, types))
    }.map(res => {
      if (res.isSuccess) res.result.count else 0L
    })
  }

  def countJob(`type`: String): Future[Long] = {
    EsClient.esClient.execute {
      count(JobReport.Index).query(termQuery(FieldKeys.FIELD_TYPE, `type`))
    }.map(res => {
      if (res.isSuccess) res.result.count else 0L
    })
  }
}
