package eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, Recovery, RecoveryCompleted}

object ActorRecovery extends App {
  val system = ActorSystem("sys")
  import RecoveryActor._

  object RecoveryActor {
    case class Command(content: String)
    case class Event(id: Int, content: String)
  }
  class RecoveryActor extends PersistentActor with ActorLogging {
    override def persistenceId: String = "recovery-actor"

    def online(latestId: Int): Receive = {
      case Command(content) => persist(Event(latestId, content)) { event =>
        log.info(s"successfullly persisted $event, recovery is ${if (this.recoveryFinished) "" else "NOT"} finished")
        context.become(online(latestId+1))
      }
    }

    override def receiveCommand: Receive = online(0)

    override def receiveRecover: Receive = {
      case RecoveryCompleted => log.info("I have finished recovery")
      case Event(id, content) =>
        log.info(s"recovered $content, recovery is ${if (this.recoveryFinished) "" else "NOT"} finished")
        context.become(online(id+1))
    }

    //    override def recovery: Recovery = Recovery(toSequenceNr = 100)
    //    override def recovery: Recovery = Recovery.none
  }

  val recoveryActor = system.actorOf(Props[RecoveryActor], "rec")
//  for (i <- 1 to 1000) {
//    recoveryActor ! Command(s"${i}")
//  }
}
