package stores

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.typesafe.config.ConfigFactory

object LocalStores extends App {
  val system = ActorSystem("sys", ConfigFactory.load().getConfig("localStores"))
  import SimplePersistentActor._
  object SimplePersistentActor {
    case object Print
    case object Snap
  }
  class SimplePersistentActor extends PersistentActor with ActorLogging {
    override def persistenceId: String = "simple-persistent-actor"
    private var nMessages = 0

    override def receiveCommand: Receive = {
      case Print => log.info(s"I have persisted $nMessages so far")
      case Snap => saveSnapshot(nMessages)
      case SaveSnapshotSuccess(metadata) => log.info(s"saving snapshot success ($metadata)")
      case SaveSnapshotFailure(metadata, cause) => log.warning(s"saving snapshot fail: ${cause} ($metadata)")
      case message => persist(message) { _ =>
        log.info(s"persisting $message")
        nMessages += 1
      }
    }

    override def receiveRecover: Receive = {
      case RecoveryCompleted =>
        log.info(s"recovery completed. total amouont of messages is $nMessages")
      case SnapshotOffer(metadata, payload: Int) =>
        log.info(s"recovered snaphot $payload ()$metadata)")
        nMessages = payload
      case message =>
        log.info(s"recovered $message")
        nMessages += 1
    }
  }

  val simplePersistentActor = system.actorOf(Props[SimplePersistentActor], "simple-persistent")
  for (i <- 1 to 10) simplePersistentActor ! s"message $i"
  simplePersistentActor ! Print
  simplePersistentActor ! Snap
  for (i <- 11 to 20) simplePersistentActor ! s"message $i"
}
