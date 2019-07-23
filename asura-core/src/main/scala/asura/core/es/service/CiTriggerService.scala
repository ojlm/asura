package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

object CiTriggerService extends CommonService {

  val logger = Logger("CiTriggerService")

  def index(doc: CiTrigger): Future[IndexDocResponse] = {
    val errorMsg = validate(doc)
    if (null == errorMsg) {
      EsClient.esClient.execute {
        indexInto(CiTrigger.Index / EsConfig.DefaultType).doc(notify).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    } else {
      errorMsg.toFutureFail
    }
  }

  def validate(doc: CiTrigger): ErrorMessage = {
    null
  }
}
