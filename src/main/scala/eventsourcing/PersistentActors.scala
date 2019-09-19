package eventsourcing

import akka.actor.{ActorLogging, ActorSystem}
import akka.persistence.PersistentActor

object PersistentActors extends App {
  val system = ActorSystem("PersistentActors")


  class Accountant extends PersistentActor with ActorLogging {
    override def persistenceId: String = "simple-accountant"

    override def receiveCommand: Receive = ???

    override def receiveRecover: Receive = ???
  }
}
