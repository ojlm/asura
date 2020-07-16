package asura.app.api

import asura.app.api.model.Dtabs
import asura.app.api.model.Dtabs.DtabItem
import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.model.{ApiRes, ApiResError}
import asura.core.http.HttpEngine
import asura.core.{CoreConfig, ErrorMessages}
import asura.namerd.DtabEntry
import asura.namerd.api.v1.NamerdV1Api
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

/** Dtab format
 * `/svc/${group}/${project}/${namespace} => /$/inet/${host}/${port}`
 */
@Singleton
class LinkerdApi @Inject()(
                            implicit val exec: ExecutionContext,
                            val controllerComponents: SecurityComponents,
                            config: Configuration,
                          ) extends BaseApi {

  val srcPrefix = "/svc/"
  val dstPrefix = "/$/inet/"

  def getProxyServers() = Action { implicit req =>
    if (CoreConfig.linkerdConfig.enabled) {
      OkApiRes(ApiRes(data = CoreConfig.linkerdConfig.servers))
    } else {
      OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_ProxyDisabled.name)))
    }
  }

  def getHttp(group: String, project: String, server: String) = Action.async { implicit req =>
    if (CoreConfig.linkerdConfig.enabled) {
      val proxyServer = CoreConfig.linkerdConfig.servers.find(_.tag.equals(server)).get
      NamerdV1Api.getNamespaceDtabs(proxyServer.namerd, proxyServer.httpNs)(HttpEngine.http).map(dtabs => {
        val items = ArrayBuffer[DtabItem]()
        dtabs.foreach(entry => {
          val pStrs = entry.prefix.split("/")
          val dStrs = entry.dst.split("/")
          if (pStrs.length == 5 && dStrs.length == 5) {
            items += DtabItem(
              group = pStrs(2),
              project = pStrs(3),
              namespace = pStrs(4),
              host = dStrs(3),
              port = dStrs(4),
              owned = group == pStrs(2) && project == pStrs(3)
            )
          }
        })
        toActionResultFromAny(items)
      })
    } else {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_ProxyDisabled.name))))
    }
  }

  def putHttp(group: String, project: String, server: String) = Action(parse.byteString).async { implicit req =>
    if (CoreConfig.linkerdConfig.enabled) {
      val proxyServer = CoreConfig.linkerdConfig.servers.find(_.tag.equals(server)).get
      val dtabs = req.bodyAs(classOf[Dtabs])
      if (null != dtabs && null != dtabs.dtabs && dtabs.dtabs.nonEmpty) {
        var error: ErrorMessage = null
        val entries = ArrayBuffer[DtabEntry]()
        for (i <- 0 until dtabs.dtabs.length if null == error) {
          val item = dtabs.dtabs(i)
          error = item.isValid()
          entries += DtabEntry(
            s"${srcPrefix}${item.group}/${item.project}/${item.namespace}",
            s"${dstPrefix}${item.host}/${item.port}"
          )
        }
        if (null == error) {
          NamerdV1Api.updateNamespaceDtabs(proxyServer.namerd, proxyServer.httpNs, entries.toSeq)(HttpEngine.http).toOkResult
        } else {
          error.toFutureFail
        }
      } else {
        Future.successful(OkApiRes(ApiRes()))
      }
    } else {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_ProxyDisabled.name))))
    }
  }
}
