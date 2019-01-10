package asura.dubbo.model

case class DubboProvider(
                          zkAddr: String,
                          path: String,
                          ref: String,
                          address: String,
                          port: Int,
                          methods: Seq[String],
                        ) {

}
