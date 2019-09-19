package eventsourcing

import java.time.LocalDateTime

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistentActors extends App {
  import Accountant._
  val system = ActorSystem("sys")

  object Accountant {
    case class Invoice(recipient: String, date: LocalDateTime, amount: Int)
    case class InvoiceRecorded(id: Int, recipient: String, date: LocalDateTime, amount: Int)
  }
  class Accountant extends PersistentActor with ActorLogging {
    private var latestId: Int = 0
    private var totalAmount: Int = 0

    override def persistenceId: String = "simple-accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        log.info(s"received invoice for amount $amount")
        persist(InvoiceRecorded(latestId, recipient, date, amount)) { event =>
          log.info(s"persisted event ${event.id} for total amount ${totalAmount+amount}")
          latestId += 1
          totalAmount += amount
        }
    }

    override def receiveRecover: Receive = {
      case InvoiceRecorded(id, _, _, amount) =>
        log.info(s"recovered event $id for amount $amount, total amount: $totalAmount")
        latestId = id
        totalAmount += amount
    }
  }

  val accountant = system.actorOf(Props[Accountant], "simple-accountant")

//  for (i <- 1 to 10) {
//    accountant ! Invoice("blah company", LocalDateTime.now(), i * 1000)
//  }
}
