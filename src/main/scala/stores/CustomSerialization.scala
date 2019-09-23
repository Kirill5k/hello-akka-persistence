package stores

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import UserRegistrationActor._
import akka.serialization.Serializer
import com.typesafe.config.ConfigFactory

class UserRegistrationSerializer extends Serializer {
  private var SEPARATOR = "//"
  override def identifier: Int = 52355 // any number will do
  override def includeManifest: Boolean = false

  override def toBinary(obj: AnyRef): Array[Byte] = obj match {
    case Registered(id, email, password) => s"[$id$SEPARATOR$email$SEPARATOR$password]".getBytes
    case _ => throw new IllegalArgumentException("only user registration events")
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val string = new String(bytes)
    val Array(id, email, password) = string.substring(1, string.length - 1).split(SEPARATOR)
    Registered(id.toInt, email, password)
  }
}

object UserRegistrationActor {
  case class Register(email: String, password: String)
  case class Registered(id: Int, email: String, password: String)
}

class UserRegistrationActor extends PersistentActor with ActorLogging {
  private var latestId = 0
  override def persistenceId: String = "user-registration-actor"
  override def receiveCommand: Receive = {
    case Register(email: String, password: String) =>
      persist(Registered(latestId, email, password)) { event =>
        log.info(s"persisted $event")
        latestId += 1
      }
  }
  override def receiveRecover: Receive = {
    case event @ Registered(id, _, _) =>
      log.info(s"recovered $event")
      latestId = id
  }

}

object CustomSerialization extends App {
  val system = ActorSystem("serialization-demo", ConfigFactory.load().getConfig("customSerialization"))
  val actor = system.actorOf(Props[UserRegistrationActor])
}
