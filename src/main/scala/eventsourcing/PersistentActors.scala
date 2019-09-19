package eventsourcing

import java.time.LocalDateTime

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistentActors extends App {
  import Accountant._
  val system = ActorSystem("sys")

  object Accountant {
    case class Invoice(recipient: String, date: LocalDateTime, amount: Int)
    case class BulkInvoice(invoices: List[Invoice])
    case class InvoiceRecorded(id: Int, recipient: String, date: LocalDateTime, amount: Int)
  }
  class Accountant extends PersistentActor with ActorLogging {
    private var latestId: Int = 0
    private var totalAmount: Int = 0

    override def persistenceId: String = "simple-accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        log.info(s"received invoice for amount $amount")
        persist(InvoiceRecorded(latestId, recipient, date, amount))(invoicePersistenceHandler)
      case BulkInvoice(invoices) =>
        log.info(s"received bulk invoice with ${invoices.size} elements")
        val invoiceIds = latestId until (latestId + invoices.size)
        val events = invoices.zip(invoiceIds).map {case (invoice, id) =>
            InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        }
        persistAll(events)(invoicePersistenceHandler)
    }

    def invoicePersistenceHandler(event: InvoiceRecorded): Unit = {
      log.info(s"persisted event ${event.id} for total amount ${totalAmount+event.amount}")
      latestId += 1
      totalAmount += event.amount
    }

    override def receiveRecover: Receive = {
      case InvoiceRecorded(id, _, _, amount) =>
        log.info(s"recovered event $id for amount $amount, total amount: $totalAmount")
        latestId = id
        totalAmount += amount
    }

    override protected def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"failed to persist $event: $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    override protected def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"persist of $event rejected: $cause")
      super.onPersistRejected(cause, event, seqNr)
    }
  }

  val accountant = system.actorOf(Props[Accountant], "simple-accountant")

  val invoices = (1 to 10).map(i => Invoice("blah company", LocalDateTime.now(), i * 1000)).toList
  accountant ! BulkInvoice(invoices)
}
