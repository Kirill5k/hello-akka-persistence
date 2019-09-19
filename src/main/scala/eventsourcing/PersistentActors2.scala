package eventsourcing

import java.util.UUID

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistentActors2 extends App {
  import VoteAggregator._
  val system = ActorSystem("PersistentActors2")

  object VoteAggregator {
    case class Vote(citizenId: String, candidate: String)
    case object ShowResults
  }

  class VoteAggregator extends PersistentActor with ActorLogging {
    private var votedCitizens: Set[String] = Set()
    private var poll: Map[String, Int] = Map()

    override def persistenceId: String = "vote-aggregator"

    def handleInternalStateChange(event: Vote): Unit = {
      votedCitizens = votedCitizens + event.citizenId
      poll = poll + (event.candidate -> (poll.getOrElse(event.candidate, 0)+1))
    }

    override def receiveCommand: Receive = {
      case vote @ Vote(citizenId, candidate) if !votedCitizens.contains(citizenId) =>
        log.info(s"citizen $citizenId voting for $candidate")
        persist(vote)(handleInternalStateChange)
      case Vote(citizenId, _) => log.warning(s"citizen $citizenId has already voted")
      case ShowResults => log.info(s"total votes count $votedCitizens. votes so far: $poll")
    }

    override def receiveRecover: Receive = {
      case vote: Vote => handleInternalStateChange(vote)
    }
  }

  val voteAggregator = system.actorOf(Props[VoteAggregator], "vote")

  voteAggregator ! Vote(UUID.randomUUID().toString, "Boris")
  voteAggregator ! Vote(UUID.randomUUID().toString, "Boris")
  voteAggregator ! Vote(UUID.randomUUID().toString, "Donald")
  voteAggregator ! Vote(UUID.randomUUID().toString, "Donald")
  voteAggregator ! Vote(UUID.randomUUID().toString, "Bill")
  voteAggregator ! Vote(UUID.randomUUID().toString, "Vladimir")
  voteAggregator ! Vote(UUID.randomUUID().toString, "Vladimir")
  voteAggregator ! Vote(UUID.randomUUID().toString, "Vladimir")
  voteAggregator ! Vote(UUID.randomUUID().toString, "Vladimir")
  voteAggregator ! Vote(UUID.randomUUID().toString, "Bill")
}
