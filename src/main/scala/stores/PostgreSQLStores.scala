package stores

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object PostgreSQLStores extends App {
  val system = ActorSystem("postgres-sys", ConfigFactory.load().getConfig("postgresqlStores"))
  import SimplePersistentActor._

  val simplePersistentActor = system.actorOf(Props[SimplePersistentActor], "simple-persistent")
  for (i <- 1 to 10) simplePersistentActor ! s"message $i"
  simplePersistentActor ! Print
  simplePersistentActor ! Snap
  for (i <- 11 to 20) simplePersistentActor ! s"message $i"
}
