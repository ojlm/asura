package asura.dubbo.model

case class DubboProvider(
                          zkConnectString: String,
                          path: String,
                          ref: String,
                          address: String,
                          port: Int,
                          methods: Seq[String],
                          application: String,
                          dubbo: String, // dubbo version
                        )
