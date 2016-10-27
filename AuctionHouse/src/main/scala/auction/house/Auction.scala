package auction.house

import akka.actor._
import akka.event.LoggingReceive
import auction.house.AuctionHouse.{ActorStopped, SellerActive}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Created by Admin on 2016-10-19.
 */
object Auction {
  case class Start(sellerAct : ActorRef, bidTimer: FiniteDuration)
  case class Bid(amount: Int, from: ActorRef){
    require(amount > 0)
  }
  case object Timeout
  case object DeleteTimeout
  case class AuctionDeleted(from: ActorRef)


  case class Sold(seller: ActorRef, buyer: ActorRef, amount: Int)
  case class BidChanged(newBuyer: ActorRef, amount: Int)
  case class InvalidBid(currentBid: Int, buyer: ActorRef)
  case class Ignored(from: ActorRef)
  case class AuctionsStopped(from: ActorRef)
}


class Auction(title: String, deleteTimer: FiniteDuration) extends Actor{
  import Auction._

  var seller: ActorRef = null
  var buyer : ActorRef = null
  var currentBid = 0

  def receive = LoggingReceive{
    case Start(sellerAct, bidTimer) =>
      seller = sellerAct
      context.system.scheduler.scheduleOnce(bidTimer, self, Timeout)
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Register(self, title)
      context become awaitFirstBid

  }

  def awaitFirstBid : Actor.Receive = LoggingReceive{
    case Bid(amount, from) =>
        buyer = from
        currentBid = amount
        context become awaitBids
    case Timeout =>
      seller ! Auction.Ignored(self)
      context.system.scheduler.scheduleOnce(deleteTimer, self, DeleteTimeout)
      context become awaitRelist
    case _ =>
  }

  def awaitBids() : Actor.Receive = LoggingReceive{
    case Bid(amount, from) if amount > currentBid=>
      buyer ! BidChanged(self, amount)
      buyer = from
      currentBid = amount
    case Bid(amount, from) =>
      from ! InvalidBid(amount, self)
    case Timeout =>
      seller ! Sold(seller, buyer, currentBid)
      buyer ! Sold(seller, buyer, currentBid)
      context.system.scheduler.scheduleOnce(deleteTimer, self, DeleteTimeout)
      context become awaitDelete
    case _ =>
  }

  def awaitRelist() : Actor.Receive = LoggingReceive{
    case Start(newSeller, bidTimer) =>
      seller = newSeller
      context.system.scheduler.scheduleOnce(bidTimer, self, Timeout)
      context become awaitFirstBid
    case DeleteTimeout =>
      println("Auction ", title, " no relist ", currentBid)
      seller ! AuctionDeleted(self)
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Remove(title)
      context.stop(self)
    case _ =>
  }

  def awaitDelete() : Actor.Receive = LoggingReceive{
    case DeleteTimeout =>
      println("Auction",title ," stopped, final amount ", currentBid)
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Remove(title)
      seller ! AuctionDeleted(self)
      context.stop(self)
    case _ =>
  }

}