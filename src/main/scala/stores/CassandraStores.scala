package stores

import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object CassandraStores extends App {
  val system = ActorSystem("postgres-sys", ConfigFactory.load().getConfig("cassandraStores"))
  import SimplePersistentActor._

  val simplePersistentActor = system.actorOf(Props[SimplePersistentActor], "simple-persistent")
  for (i <- 1 to 10) simplePersistentActor ! Message(s"message $i", LocalDateTime.now(), "GENERIC_MESSAGE")
  simplePersistentActor ! Print
  simplePersistentActor ! Snap
  for (i <- 11 to 20) simplePersistentActor ! Message(s"message $i", LocalDateTime.now(), "GENERIC_MESSAGE")
}
