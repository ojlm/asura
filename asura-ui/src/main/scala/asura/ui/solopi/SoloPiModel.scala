package asura.ui.solopi

import asura.common.util.JsonUtils

case class SoloPiModel(
                        caseName: String,
                        caseDesc: String,
                        gmtCreate: Long,
                        gmtModify: Long,
                        targetAppLabel: String,
                        targetAppPackage: String,
                      ) {

}

object SoloPiModel {

  def parse(raw: String): SoloPiModel = {
    JsonUtils.parse(raw, classOf[SoloPiModel])
  }

}
