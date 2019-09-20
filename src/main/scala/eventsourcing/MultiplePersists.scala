package eventsourcing

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor


object MultiplePersists extends App {
  import DiligentAccountant._

  val system = ActorSystem("sys")

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"received $message")
    }
  }

  object DiligentAccountant {
    case class Invoice(recipient: String, date: LocalDateTime, amount: Int)
    case class TaxRecord(taxId: String, recordId: Int, date: LocalDateTime, totalAmount: Int)
    case class InvoiceRecord(id: Int, recipient: String, date: LocalDateTime, amount: Int)
    case class Declaration(message: String)

    def props(taxId: String, taxAuthority: ActorRef) = Props(new DiligentAccountant(taxId, taxAuthority))
  }

  class DiligentAccountant(taxId: String, taxAuthority: ActorRef) extends PersistentActor with ActorLogging {
    private var latestTaxRecordId = 0
    private var latestInvoiceId = 0

    override def persistenceId: String = "diligent-accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        persist(TaxRecord(taxId, 0, date, amount / 3)) { record =>
          taxAuthority ! record
          latestTaxRecordId += 1
          persist(Declaration("I hereby declare this tax record to be true and complete.")) { declaration =>
            taxAuthority ! declaration
          }
        }
        persist(InvoiceRecord(latestInvoiceId, recipient, date, amount)) { record =>
          taxAuthority ! record
          latestInvoiceId += 1
          persist(Declaration("I hereby declare this invoice record to be true and complete.")) { declaration =>
            taxAuthority ! declaration
          }
        }
    }

    override def receiveRecover: Receive = {
      case event => log.info(s"recovered $event")
    }
  }

  val taxAuthority = system.actorOf(Props[TaxAuthority], "HMRC")
  val accountant = system.actorOf(DiligentAccountant.props("UK5321_521", taxAuthority))

  accountant ! Invoice("blah company", LocalDateTime.now(), 2000)
}
