package asura.core.cs

import asura.core.es.model.JobReportData.CaseReportItemMetrics

class CaseRuntimeMetrics {

  private val scale: Double = 1000000L.toDouble
  private var _start: Long = 0L
  private var _renderRequestStart = 0L
  private var _renderRequestEnd = 0L
  private var _renderAuthStart = 0L
  private var _renderAuthEnd = 0L
  private var _performRequestStart = 0L
  private var _evalAssertionStart = 0L
  private var _evalAssertionEnd = 0L
  private var _theEnd: Long = 0L

  def start(): CaseRuntimeMetrics = {
    _start = System.nanoTime()
    this
  }

  def renderRequestStart(): CaseRuntimeMetrics = {
    _renderRequestStart = System.nanoTime()
    this
  }

  def renderRequestEnd(): CaseRuntimeMetrics = {
    _renderRequestEnd = System.nanoTime()
    this
  }

  def renderAuthBegin(): CaseRuntimeMetrics = {
    _renderAuthStart = System.nanoTime()
    this
  }

  def performRequestStart(): CaseRuntimeMetrics = {
    _performRequestStart = System.nanoTime()
    this
  }

  def evalAssertionBegin(): CaseRuntimeMetrics = {
    _evalAssertionStart = System.nanoTime()
    this
  }

  def evalAssertionEnd(): CaseRuntimeMetrics = {
    _evalAssertionEnd = System.nanoTime()
    this
  }

  def renderAuthEnd(): CaseRuntimeMetrics = {
    _renderAuthEnd = System.nanoTime()
    this
  }

  def theEnd(): CaseRuntimeMetrics = {
    _theEnd = System.nanoTime()
    this
  }

  def toReportItemMetrics(): CaseReportItemMetrics = {
    CaseReportItemMetrics(
      renderRequestTime = Math.round((_renderRequestEnd - _renderRequestStart).toDouble / scale),
      renderAuthTime = Math.round((_renderAuthEnd - _renderAuthStart).toDouble / scale),
      requestTime = Math.round((_evalAssertionStart - _performRequestStart).toDouble / scale),
      evalAssertionTime = Math.round((_evalAssertionEnd - _evalAssertionStart).toDouble / scale),
      totalTime = Math.round((_theEnd - _start).toDouble / scale)
    )
  }
}

object CaseRuntimeMetrics {

  def apply(): CaseRuntimeMetrics = {
    val metrics = new CaseRuntimeMetrics()
    metrics.start()
  }
}
