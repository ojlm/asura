package asura.core.es.model

case class JobReportDataStatistic(
                                   caseCount: Int, // in cases
                                   caseOK: Int, // in cases
                                   caseKO: Int, // in cases
                                   scenarioCount: Int,
                                   scenarioOK: Int, // in scenarios
                                   scenarioKO: Int, // in scenarios
                                   scenarioCaseCount: Int, // in scenarios
                                   scenarioCaseOK: Int, // in scenarios passed
                                   scenarioCaseKO: Int, // in scenarios failed
                                   scenarioCaseOO: Int, // in scenarios skipped
                                   okRate: Int, // (caseOk + scenarioOK) / (caseCount + scenarioCount)
                                   assertionPassed: Int, // all
                                   assertionFailed: Int, // all
                                 )

object JobReportDataStatistic {

  def apply(reportData: JobReportData): JobReportDataStatistic = {
    var caseOK, caseKO = 0
    var scenarioOK, scenarioKO, scenarioCaseCount, scenarioCaseOK, scenarioCaseKO, scenarioCaseOO = 0
    var assertionPassed, assertionFailed = 0
    val caseCount = reportData.cases.length
    reportData.cases.foreach(c => {
      if (c.isSuccessful()) {
        caseOK = caseOK + 1
      } else {
        caseKO = caseKO + 1
      }
      val statis = c.statis
      assertionPassed = assertionPassed + statis.passed
      assertionFailed = assertionFailed + statis.failed
    })
    val scenarioCount = reportData.scenarios.length
    reportData.scenarios.foreach(s => {
      if (s.isSuccessful()) {
        scenarioOK = scenarioOK + 1
      } else {
        scenarioKO = scenarioKO + 1
      }
      scenarioCaseCount = scenarioCaseCount + s.cases.length
      s.cases.foreach(c => {
        if (c.isSuccessful()) {
          scenarioCaseOK = scenarioCaseOK + 1
        } else if (c.isFailed()) {
          scenarioCaseKO = scenarioCaseKO + 1
        } else {
          scenarioCaseOO = scenarioCaseOO + 1
        }
        val statis = c.statis
        assertionPassed = assertionPassed + statis.passed
        assertionFailed = assertionFailed + statis.failed
      })
    })
    JobReportDataStatistic(
      caseCount = caseCount,
      caseOK = caseOK,
      caseKO = caseKO,
      scenarioCount = scenarioCount,
      scenarioOK = scenarioOK,
      scenarioKO = scenarioKO,
      scenarioCaseCount = scenarioCaseCount,
      scenarioCaseOK = scenarioCaseOK,
      scenarioCaseKO = scenarioCaseKO,
      scenarioCaseOO = scenarioCaseOO,
      okRate = Math.round(((caseOK + scenarioOK) * 100).toDouble / (caseCount + scenarioCount).toDouble).toInt,
      assertionPassed = assertionPassed,
      assertionFailed = assertionFailed
    )
  }
}
