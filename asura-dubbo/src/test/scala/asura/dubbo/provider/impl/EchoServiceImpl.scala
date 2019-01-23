package asura.dubbo.provider.impl

import asura.dubbo.model.Person
import asura.dubbo.service.EchoService

class EchoServiceImpl extends EchoService {

  override def echoString(text: String, num: Int): String = s"Hello ${text}, ${num}"

  override def echoPerson(person: Person): Person = person

  override def echoListString(list: Seq[String]): Seq[String] = list

  override def echoListPerson(list: Seq[Person]): Seq[Person] = list

}
