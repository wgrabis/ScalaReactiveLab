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
case class InitialData(auctionHouse: ActorRef, deleteTimer: FiniteDuration) extends Data
case class FinalData(auctionHouse: ActorRef, deleteTimer: FiniteDuration, lastBid: Int) extends Data
case class AuctionData(currentBid: Int, buyer: ActorRef, seller: ActorRef, auctionHouse: ActorRef, deleteTimer: FiniteDuration) extends Data


class AuctionFSM(auctionHouse: ActorRef, deleteTimer: FiniteDuration) extends FSM[State, Data] {
  startWith(PreInit, InitialData(auctionHouse, deleteTimer))

  when(PreInit){
    case Event(Start(sellerAct, bidTimer), InitialData(auctionHouse, deleteTimer))=>
      context.system.scheduler.scheduleOnce (bidTimer, self, Timeout)
      auctionHouse ! SellerActive
      goto (Created) using AuctionData(0, null, sellerAct, auctionHouse, deleteTimer)
  }

  when(Created){
    case Event(Bid(amount, from), AuctionData(currentBid, buyer, seller, auctionHouse, deleteTimer)) =>
      goto (Activated) using AuctionData (amount, from, seller, auctionHouse, deleteTimer)
    case Event(Timeout, AuctionData(currentBid, buyer, seller, auctionHouse, deleteTimer)) =>
      context.system.scheduler.scheduleOnce(deleteTimer, self, DeleteTimeout)
      goto (Ignored) using AuctionData (currentBid, buyer, seller, auctionHouse, deleteTimer)
    case _ =>
      stay
  }

  when(Ignored){
    case Event(Start(newSeller, bidTimer),  AuctionData(currentBid, buyer, seller, auctionHouse, deleteTimer))=>
      context.system.scheduler.scheduleOnce (bidTimer, self, Timeout)
      goto (Created) using AuctionData(0, null, newSeller, auctionHouse, deleteTimer)
    case Event(DeleteTimeout, AuctionData(currentBid, buyer, seller, auctionHouse, deleteTimer)) =>
      println("Auction stopped, no relist ")
      auctionHouse ! ActorStopped(self)
      stop()
    case _ =>
      stay
  }

  when(Activated){
    case Event(Bid(amount, from), AuctionData(currentBid, buyer, seller, auctionHouse, deleteTimer)) if amount > currentBid=>
      buyer ! BidChanged(self, amount)
      stay using AuctionData(amount, from, seller, auctionHouse, deleteTimer)
    case Event(Bid(amount, from), AuctionData(currentBid, buyer, seller, auctionHouse, deleteTimer)) =>
      from ! InvalidBid(amount, self)
      stay
    case Event(Timeout, AuctionData(currentBid, buyer, seller, auctionHouse, deleteTimer)) =>
      seller ! Sold(seller, buyer, currentBid)
      buyer ! Sold(seller, buyer, currentBid)
      context.system.scheduler.scheduleOnce(deleteTimer, self, DeleteTimeout)
      goto (SoldS) using FinalData(auctionHouse, deleteTimer, currentBid)
    case _ =>
      stay
  }

  when(SoldS){
    case Event(DeleteTimeout,FinalData(auctionHouse, deleteTimer, finalBid)) =>
      println("Auction stopped, final amount ", finalBid)
      auctionHouse ! ActorStopped(self)
      stop()
    case _ =>
      stay
  }
}
