package asura.namerd

import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import asura.common.AkkaTestKitBaseSpec
import asura.common.util.FutureUtils.RichFuture
import asura.common.util.JsonUtils
import asura.namerd.api.v1.NamerdV1Api

class NamerdV1ApiSpec extends AkkaTestKitBaseSpec {

  NamerdConfig.init(
    "http://localhost:4180",
    system,
    system.dispatcher,
    ActorMaterializer()
  )
  implicit val http = Http()

  "get all ns" in {
    val ns = NamerdV1Api.getAllNamespaces().await
    println(ns)
  }

  "create dtabs" in {
    val dtabs = Seq(
      DtabEntry("/svc/web01", "/$/inet/127.1/9998"),
      DtabEntry("/svc/web02", "/$/inet/127.1/9999")
    )
    val ns = NamerdV1Api.createNamespaceDtabs("test", dtabs).await
    println(ns)
  }

  "get dtabs" in {
    val ns = NamerdV1Api.getNamespaceDtabs("test").await
    println(JsonUtils.stringify(ns))
  }

  "update dtabs" in {
    val dtabs = Seq(
      DtabEntry("/svc/web01", "/$/inet/127.1/1998"),
      DtabEntry("/svc/web02", "/$/inet/127.1/9999")
    )
    val ns = NamerdV1Api.updateNamespaceDtabs("test", dtabs).await
    println(JsonUtils.stringify(ns))
  }

  "delete dtabs" in {
    val ns = NamerdV1Api.deleteNamespaceDtabs("test").await
    println(ns)
  }
}
