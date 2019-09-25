package patterns

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventAdapter, EventSeq}
import com.typesafe.config.ConfigFactory
import patterns.DomainModel.CouponApplied

import scala.collection.mutable
import scala.util.Random

object DomainModel {
  case class User(id: String, email: String, name: String)
  case class Coupon(code: String, promotionAmount: Int)

  case class ApplyCoupon(coupon: Coupon, user: User)
  case class CouponApplied(code: String, user: User)
}

object DataModel {
  case class WrittenCouponApplied(code: String, userId: String, userEmail: String)
  case class WrittenCouponAppliedV2(code: String, userId: String, userEmail: String, userName: String)
}

class ModelAdapter extends EventAdapter {
  import DomainModel._
  import DataModel._

  override def manifest(event: Any): String = ???

  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case WrittenCouponApplied(code, userId, userEmail) => EventSeq.single(CouponApplied(code, User(userId, userEmail, "")))
    case WrittenCouponAppliedV2(code, userId, userEmail, userName) => EventSeq.single(CouponApplied(code, User(userId, userEmail, userName)))
    case other => EventSeq.single(other)
  }

  override def toJournal(event: Any): Any = event match {
    case CouponApplied(code, user) => WrittenCouponAppliedV2(code, user.id, user.email, user.name)
  }
}

object DetachingModels extends App {
  import DomainModel._

  class CouponManager extends PersistentActor with ActorLogging {
    override def persistenceId: String = "coupon-manager"

    private val coupons: mutable.Map[String, User] = mutable.Map()

    override def receiveCommand: Receive = {
      case ApplyCoupon(coupon, user) if !coupons.contains(coupon.code) =>
        persist(CouponApplied(coupon.code, user)) { event =>
          log.info(s"persisted $event")
          coupons.put(coupon.code, user)
        }
    }

    override def receiveRecover: Receive = {
      case event @ CouponApplied(code, user) =>
        log.info(s"recovered $event")
        coupons.put(code, user)
    }
  }

  val system = ActorSystem("detachingModels", ConfigFactory.load().getConfig("detachingModels"))
  val couponManager = system.actorOf(Props[CouponManager])

  val coupons = for (i <- 1 to 5) yield Coupon(s"mega_coupon_$i", 100)
  val users = for (i <- 1 to 5) yield User(s"$i", s"user_$i@text.com", s"user_$i")
  coupons.zip(users).foreach{case (user, coupon) => couponManager ! ApplyCoupon(user, coupon)}
}
