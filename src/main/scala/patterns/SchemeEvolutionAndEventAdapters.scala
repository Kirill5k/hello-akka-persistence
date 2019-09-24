package patterns

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventSeq, ReadEventAdapter}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.util.Random

object SchemeEvolutionAndEventAdapters extends App {
  import InventoryManager._

  object InventoryManager {
    val ACOUSTIC = "acoustic"
    val ELECTRIC = "electric"

    case object DisplayInventory
    case class Guitar(id: String, model: String, make: String, guitarType: String)
    case class AddGuitar(guitar: Guitar, quantity: Int)
    case class GuitarAdded(guitarId: String, guitarModel: String, guitarMake: String, quantity: Int)
    case class TypedGuitarAdded(guitarId: String, guitarModel: String, guitarMake: String, guitarType: String, quantity: Int)
  }

  class GuitarReadEventAdapter extends ReadEventAdapter {
    override def fromJournal(event: Any, manifest: String): EventSeq = event match {
      case GuitarAdded(id, model, make, quantity) =>
        EventSeq.single(TypedGuitarAdded(id, model, make, ACOUSTIC, quantity))
      case other => EventSeq.single(other)
    }
  }

  class InventoryManager extends PersistentActor with ActorLogging {
    override def persistenceId: String = "guitar-inventory-manager"

    private val inventory: mutable.Map[Guitar, Int] = new mutable.HashMap[Guitar, Int]()

    override def receiveCommand: Receive = {
      case DisplayInventory => log.info(s"current inventory $inventory")
      case AddGuitar(guitar @ Guitar(id, make, model, guitarType), quantity) =>
        persist(TypedGuitarAdded(id, make, model, guitarType, quantity)) { _ =>
          updateInventory(guitar, quantity)
          log.info(s"added $quantity x $guitar to the inventory")
        }
    }

    override def receiveRecover: Receive = {
      case TypedGuitarAdded(id, model, make, guitarType, quantity) =>
        log.info(s"recovered guitar with type")
        updateInventory(Guitar(id, model, make, guitarType), quantity)
    }

    def updateInventory(guitar: Guitar, quantity: Int): Unit = inventory.get(guitar) match {
      case None => inventory.put(guitar, quantity)
      case Some(initialQuantity) => inventory(guitar) = initialQuantity + quantity
    }
  }

  val random = Random
  val system = ActorSystem("event-adapters", ConfigFactory.load().getConfig("eventAdapters"))
  val inventoryManager = system.actorOf(Props[InventoryManager], "inv-manager")

  val guitars = for (i <- 1 to 10) yield Guitar(s"$i", s"GT$i-X", s"AkkaXM", if (random.nextBoolean()) ELECTRIC else ACOUSTIC)
  guitars.foreach(guitar => inventoryManager ! AddGuitar(guitar, random.nextInt(5)+1))
  inventoryManager ! DisplayInventory
}
