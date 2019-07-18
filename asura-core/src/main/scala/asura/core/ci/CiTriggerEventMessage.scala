package asura.core.ci

case class CiTriggerEventMessage(
                                  group: String,
                                  project: String,
                                  env: String,
                                  author: String,
                                  service: String,
                                  `type`: String,
                                )
