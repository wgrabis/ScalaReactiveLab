package auction.house

import akka.actor.{ActorRef, FSM}
import akka.util.Timeout
import auction.house.Auction._
import auction.house.AuctionHouse.{ActorStopped, SellerActive}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Admin on 2016-10-20.
  */
sealed trait State
case object PreInit extends State
case object Created extends State
case object Ignored extends State
case object Activated extends State
case object SoldS extends State

sealed trait Data
case class InitialData(title: String) extends Data
case class FinalData(seller: ActorRef, title: String, lastBid: Int) extends Data
case class AuctionData(currentBid: Int, buyer: ActorRef, seller: ActorRef, title: String) extends Data


class AuctionFSM(title: String, deleteTimer: FiniteDuration) extends FSM[State, Data] {
  startWith(PreInit, InitialData(title))

  when(PreInit){
    case Event(Start(sellerAct, bidTimer), InitialData(title))=>
      context.system.scheduler.scheduleOnce (bidTimer, self, Timeout)
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Register(self, title)
      goto (Created) using AuctionData(0, null, sellerAct, title)
  }

  when(Created){
    case Event(Bid(amount, from), AuctionData(currentBid, buyer, seller, title)) =>
      goto (Activated) using AuctionData(amount, from, seller, title)
    case Event(Timeout, AuctionData(currentBid, buyer, seller, title)) =>
      seller ! Auction.Ignored(self)
      context.system.scheduler.scheduleOnce(deleteTimer, self, DeleteTimeout)
      goto (Ignored) using AuctionData(currentBid, buyer, seller, title)
    case Event(DeleteTimeout, _) =>
      stay
  }

  when(Ignored){
    case Event(Start(newSeller, bidTimer),  AuctionData(currentBid, buyer, seller, title))=>
      context.system.scheduler.scheduleOnce (bidTimer, self, Timeout)
      goto (Created) using AuctionData(0, null, newSeller, title)
    case Event(DeleteTimeout, AuctionData(currentBid, buyer, seller, title)) =>
      println("Auction stopped, no relist ")
      seller ! AuctionDeleted(self)
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Remove(title)
      stop()
  }

  when(Activated){
    case Event(Bid(amount, from), AuctionData(currentBid, buyer, seller, title)) if amount > currentBid=>
      buyer ! BidChanged(self, amount)
      stay using AuctionData(amount, from, seller, title)
    case Event(Bid(amount, from), AuctionData(currentBid, buyer, seller, title)) =>
      from ! InvalidBid(amount, self)
      stay
    case Event(Timeout, AuctionData(currentBid, buyer, seller, title)) =>
      seller ! Sold(seller, buyer, currentBid)
      buyer ! Sold(seller, buyer, currentBid)
      context.system.scheduler.scheduleOnce(deleteTimer, self, DeleteTimeout)
      goto (SoldS) using FinalData(seller, title, currentBid)
    case Event(DeleteTimeout, _) =>
      stay
  }

  when(SoldS){
    case Event(DeleteTimeout,FinalData(seller, title, finalBid)) =>
      println("Auction",title ," stopped, final amount ", finalBid)
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Remove(title)
      seller ! AuctionDeleted(self)
      stop()
  }
}
