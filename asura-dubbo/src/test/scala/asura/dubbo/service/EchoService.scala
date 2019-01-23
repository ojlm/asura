package asura.dubbo.service

import asura.dubbo.model.Person

trait EchoService {

  def echoString(text: String, num: Int): String

  def echoPerson(person: Person): Person

  def echoListString(list: Seq[String]): Seq[String]

  def echoListPerson(list: Seq[Person]): Seq[Person]
}
