package patterns

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.journal.{Tagged, WriteEventAdapter}
import akka.persistence.query.{Offset, PersistenceQuery}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.util.Random

object PersistenceQueries extends App {

  val system = ActorSystem("persistenceQueries", ConfigFactory.load().getConfig("persistenceQueries"))

  val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  val persistenceIds = readJournal.persistenceIds()

  implicit val materializer = ActorMaterializer()(system)
  persistenceIds.runForeach { persistenceId =>
    println(s"found persistence id: $persistenceId")
  }

  // events by persistent id
  val events = readJournal.eventsByPersistenceId("guitar-inventory-manager", 0, Long.MaxValue)
  events.runForeach { event =>
    println(s"read event $event")
  }

  // events by tags

  val genres = Array("pop", "rock", "hip-hop", "jazz", "disco")
  case class Song(artist: String, title: String, genre: String)
  case class Playlist(songs: List[Song])
  case class PlaylistPurchased(id: Int, songs: List[Song])

  class MusicStoreCheckoutActor extends PersistentActor with ActorLogging {
    override def persistenceId: String = "music-store-checkout"
    private var latestId = 0

    override def receiveCommand: Receive = {
      case Playlist(songs) => persist(PlaylistPurchased(latestId+1, songs)) { _ =>
        log.info(s"playlist purchased $songs")
        latestId += 1
      }
    }
    override def receiveRecover: Receive = {
      case event @ PlaylistPurchased(id, _) =>
        log.info(s"recovered playlist $event")
        latestId = id
    }
  }

  class MusicStoreEventAdapter extends WriteEventAdapter {
    override def manifest(event: Any): String = "musicSstore"

    override def toJournal(event: Any): Any = event match {
      case event @ PlaylistPurchased(_, songs) =>
        val genres = songs.map(_.genre).toSet
        Tagged(event, genres)
      case event => event
    }
  }

  val musicStoreActor = system.actorOf(Props[MusicStoreCheckoutActor], "music-store")
  val rand = Random
  for (_ <- 1 to 10) {
    val maxSongs = rand.nextInt(15)
    val songs = for (i <- 1 to maxSongs) yield Song(s"Boris X-$i", s"Song $i", genres(rand.nextInt(5)))
    musicStoreActor ! Playlist(songs.toList)
  }

  val rockPlaylists = readJournal.eventsByTag("rock", Offset.noOffset)
  rockPlaylists.runForeach { event =>
    println(s"found rock playlist $event")
  }
}
