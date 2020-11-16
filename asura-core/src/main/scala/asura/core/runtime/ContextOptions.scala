package asura.core.runtime

import java.util

import asura.common.util.StringUtils
import asura.core.es.model.Environment

/** used for override default case runtime behavior
 *
 * 1. get which `Environment` is used.
 * 2. other options maybe added in the future
 */
case class ContextOptions(
                           var jobEnv: String = null,
                           var scenarioEnv: String = null,
                           var stepEnv: String = null,
                           var initCtx: util.Map[Any, Any] = null,
                         ) {

  private var cachedEnvironments: util.concurrent.ConcurrentMap[String, Environment] = null

  def getUsedEnv(id: String = getUsedEnvId()): Environment = {
    if (null != cachedEnvironments && null != id) {
      cachedEnvironments.get(id)
    } else {
      null
    }
  }

  def setUsedEnv(id: String, env: Environment): Environment = {
    if (null == cachedEnvironments) {
      cachedEnvironments = new util.concurrent.ConcurrentHashMap[String, Environment]()
    }
    cachedEnvironments.put(id, env)
  }

  def getUsedEnvId(): String = {
    if (StringUtils.isNotEmpty(jobEnv)) {
      jobEnv
    } else if (StringUtils.isNotEmpty(scenarioEnv)) {
      scenarioEnv
    } else {
      stepEnv
    }
  }
}
