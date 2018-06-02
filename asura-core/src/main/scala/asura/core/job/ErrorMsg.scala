package asura.core.job

object ErrorMsg {

  val ERROR_UNHANDLED = "存在未处理的任务异常,需要人工处理,陛下~~"
  val ERROR_FILTERED = "被过滤啦~~"
  val ERROR_INIT = "初始化~~"
  val ERROR_SUCCESS = "任务执行成功~~"
  val ERROR_INVALID_JOB_META = "需要任务元数据~~"
  val ERROR_INVALID_SCHEDULER_TYPE = "无效的调度器~~"
  val ERROR_INVALID_JOB_CLASS = "无法找到对应任务类型~~"
}
