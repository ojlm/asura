package asura.core.job

import akka.testkit.TestProbe
import asura.common.AkkaTestKitBaseSpec
import asura.core.job.eventbus.JobStatusBus
import asura.core.job.eventbus.JobStatusBus.JobStatusNotificationMessage

import scala.concurrent.duration._

class JobStatusBusSpec extends AkkaTestKitBaseSpec {

  "subscribe test" in {
    val observer1 = TestProbe().ref
    val observer2 = TestProbe().ref
    val probe1 = TestProbe()
    val probe2 = TestProbe()
    val subscriber1 = probe1.ref
    val subscriber2 = probe2.ref
    val jobStatusBus = new JobStatusBus(system)
    jobStatusBus.subscribe(subscriber1, observer1)
    jobStatusBus.subscribe(subscriber2, observer1)
    jobStatusBus.subscribe(subscriber2, observer2)
    jobStatusBus.publish(JobStatusNotificationMessage(observer1, "add", "scheduler", "group", "name"))
    probe1.expectMsg(JobStatusNotificationMessage(observer1, "add", "scheduler", "group", "name"))
    probe2.expectMsg(JobStatusNotificationMessage(observer1, "add", "scheduler", "group", "name"))
    jobStatusBus.publish(JobStatusNotificationMessage(observer2, "del", "scheduler", "group", "name"))
    probe2.expectMsg(JobStatusNotificationMessage(observer2, "del", "scheduler", "group", "name"))
    probe1.expectNoMessage(500.millis)
  }
}
