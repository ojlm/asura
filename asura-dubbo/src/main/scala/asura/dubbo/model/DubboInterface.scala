package asura.dubbo.model

case class DubboInterface(
                           zkConnectString: String,
                           path: String,
                           ref: String,
                         )
