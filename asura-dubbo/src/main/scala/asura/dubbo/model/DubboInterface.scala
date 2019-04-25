package asura.dubbo.model

case class DubboInterface(
                           zkAddr: String,
                           zkPort: Int,
                           path: String,
                           ref: String,
                         ) {

}
