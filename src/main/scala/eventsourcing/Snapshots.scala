package eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

import scala.collection.mutable

object Snapshots extends App {
  import Chat._

  object Chat {
    case object Print
    case class SentMessage(content: String)
    case class ReceivedMessage(content: String)

    case class SentMessageRecord(id: Int, content: String)
    case class ReceivedMessageRecord(id: Int, content: String)

    def props(owner: String, contact: String) = Props(new Chat(owner, contact))
  }

  class Chat(owner: String, contact: String) extends PersistentActor with ActorLogging {
    private val MAX_MESSAGES = 10
    private var currentMessageId = 0
    private var commandsWithoutCheckpoint = 0
    private val lastMessages = new mutable.Queue[(String, String)]()

    override def persistenceId: String = s"${owner}-${contact}-chat"

    override def receiveCommand: Receive = {
      case Print =>
        log.info("showing latest message:")
        lastMessages.foreach(tup => log.info(s"${tup._1}: ${tup._2}"))
      case ReceivedMessage(content) => persist(ReceivedMessageRecord(currentMessageId, content)) { _ =>
        log.info(s"received message $content")
        enqueueMessage(contact, content)
        currentMessageId += 1
        checkpoint()
      }
      case SentMessage(content) => persist(SentMessageRecord(currentMessageId, content)) { _ =>
        log.info(s"sent message $content")
        enqueueMessage(owner, content)
        currentMessageId += 1
        checkpoint()
      }
      case SaveSnapshotSuccess(metadata) => log.info(s"saved snapshot $metadata")
      case SaveSnapshotFailure(metadata, cause) => log.info(s"failed to save a snapshot $metadata: ${cause.getMessage}")
    }

    def enqueueMessage(originator: String, content: String): Unit = {
      if (lastMessages.size >= MAX_MESSAGES) lastMessages.dequeue()
      lastMessages.enqueue((originator, content))
    }

    def checkpoint(): Unit = {
      commandsWithoutCheckpoint += 1
      if (commandsWithoutCheckpoint >= MAX_MESSAGES) {
        log.info("saving checkpoint")
        saveSnapshot(lastMessages)
        commandsWithoutCheckpoint = 0
      }
    }

    override def receiveRecover: Receive = {
      case ReceivedMessageRecord(id, content) =>
        log.info(s"recovered received message $id $content")
        enqueueMessage(content, content)
        currentMessageId = id
      case SentMessageRecord(id, content) =>
        log.info(s"recovered sent message $id $content")
        enqueueMessage(owner, content)
        currentMessageId = id
      case SnapshotOffer(_, contents) =>
        log.info("recovered snapshot")
        contents.asInstanceOf[mutable.Queue[(String, String)]].foreach(lastMessages.enqueue(_))
    }
  }

  val system = ActorSystem("system")
  val chat = system.actorOf(Chat.props("user-1", "user-2"))
}
