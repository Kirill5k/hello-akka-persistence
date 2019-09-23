package stores

import java.time.LocalDateTime
import java.util.UUID

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

object SimplePersistentActor {
  case object Print
  case object Snap
  case class Message(content: String, date: LocalDateTime, status: String)
  case class Record(id: UUID, content: String, date: LocalDateTime, status: String)
}

class SimplePersistentActor extends PersistentActor with ActorLogging {
  import SimplePersistentActor._
  override def persistenceId: String = "simple-persistent-actor"
  private var nMessages = 0

  override def receiveCommand: Receive = {
    case Print => log.info(s"I have persisted $nMessages so far")
    case Snap => saveSnapshot(nMessages)
    case SaveSnapshotSuccess(metadata) => log.info(s"saving snapshot success ($metadata)")
    case SaveSnapshotFailure(metadata, cause) => log.warning(s"saving snapshot fail: ${cause} ($metadata)")
    case Message(content, date, status) => persist(Record(UUID.randomUUID(), content, date, status)) { record =>
      log.info(s"persisting $record")
      nMessages += 1
    }
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
