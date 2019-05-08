package asura.core.runtime

import asura.core.es.model.JobReportData.JobReportStepItemMetrics

class RuntimeMetrics {

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

  def start(): RuntimeMetrics = {
    _start = System.nanoTime()
    this
  }

  def renderRequestStart(): RuntimeMetrics = {
    _renderRequestStart = System.nanoTime()
    this
  }

  def renderRequestEnd(): RuntimeMetrics = {
    _renderRequestEnd = System.nanoTime()
    this
  }

  def renderAuthBegin(): RuntimeMetrics = {
    _renderAuthStart = System.nanoTime()
    this
  }

  def performRequestStart(): RuntimeMetrics = {
    _performRequestStart = System.nanoTime()
    this
  }

  def evalAssertionBegin(): RuntimeMetrics = {
    _evalAssertionStart = System.nanoTime()
    this
  }

  def evalAssertionEnd(): RuntimeMetrics = {
    _evalAssertionEnd = System.nanoTime()
    this
  }

  def renderAuthEnd(): RuntimeMetrics = {
    _renderAuthEnd = System.nanoTime()
    this
  }

  def theEnd(): RuntimeMetrics = {
    _theEnd = System.nanoTime()
    this
  }

  def toReportStepItemMetrics(): JobReportStepItemMetrics = {
    JobReportStepItemMetrics(
      renderRequestTime = Math.round((_renderRequestEnd - _renderRequestStart).toDouble / scale),
      renderAuthTime = Math.round((_renderAuthEnd - _renderAuthStart).toDouble / scale),
      requestTime = Math.round((_evalAssertionStart - _performRequestStart).toDouble / scale),
      evalAssertionTime = Math.round((_evalAssertionEnd - _evalAssertionStart).toDouble / scale),
      totalTime = Math.round((_theEnd - _start).toDouble / scale)
    )
  }
}

object RuntimeMetrics {

  def apply(): RuntimeMetrics = {
    val metrics = new RuntimeMetrics()
    metrics.start()
  }
}
