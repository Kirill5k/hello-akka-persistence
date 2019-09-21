package eventsourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

object AsyncPersist extends App {
  val system = ActorSystem("sys")
  import StreamProcessor._

  object StreamProcessor {
    case class Command(content: String)
    case class Event(content: String)

    def props(eventAggregator: ActorRef) = Props(new StreamProcessor(eventAggregator))
  }

  class StreamProcessor(eventAggregator: ActorRef) extends PersistentActor with ActorLogging {
    override def persistenceId: String = "stream-processor"

    override def receiveCommand: Receive = {
      case Command(content) =>
        eventAggregator ! s"processing $content"
        persistAsync(Event(content)) { event =>
          eventAggregator ! event
        }
        val processedContents = content + "_processed"
        persistAsync(Event(processedContents)) { event =>
          eventAggregator ! event
        }
    }

    override def receiveRecover: Receive = {
      case message => log.info(s"recovered $message")
    }

  }

  class EventAggregator extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"aggregating $message")
    }
  }

  val eventAggregator = system.actorOf(Props[EventAggregator], "agg")
  val streamProcessor = system.actorOf(props(eventAggregator), "processor")
}
