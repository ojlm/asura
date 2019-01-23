package asura.dubbo.model

import asura.dubbo.model.InterfaceMethodParams.MethodSignature

case class InterfaceMethodParams(
                                  ref: String,
                                  methods: Seq[MethodSignature]
                                ) {

}

object InterfaceMethodParams {

  case class MethodSignature(
                              ret: String,
                              method: String,
                              params: Seq[String],
                            )

}
