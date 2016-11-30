package auction.house

import java.util

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import auction.house.Auction.{AuctionDeleted, Sold, Start}
import auction.house.AuctionHouse.{ActorStopped, SellerActive}
import auction.house.Seller._
import auction.house.persistent.PersistentAuction

import scala.concurrent.duration.FiniteDuration

/**
  * Created by Admin on 2016-10-19.
  */
object Seller{
  case object StartAuction
  case object StartAuctionFSM
  case object StartPersist
  case object StartTest
  case class AskForAuction(from: ActorRef)
}

class Seller(bidTime: FiniteDuration, var timesReList: Int, auctionItems: List[String]) extends Actor{

  var auctionSize = auctionItems.size

  def receive : Actor.Receive= LoggingReceive{
    case StartAuction =>
      for(item <- auctionItems) {
        val actor = context.actorOf(Props(new Auction(item, FiniteDuration(2, "seconds"))))
        actor ! Start(self, bidTime)
      }
      context.actorSelection("/user/mainActor") ! SellerActive
      context become awaitForAuction

    case StartAuctionFSM =>
      for(item <- auctionItems) {
        val actor = context.actorOf(Props(new AuctionFSM(item, FiniteDuration(2, "seconds"))))
        actor ! Start(self, bidTime)
      }
      context.actorSelection("/user/mainActor") ! SellerActive
      context become awaitForAuction

    case StartPersist =>
      for(item <- auctionItems) {
        val actor = context.actorOf(Props(new PersistentAuction(item, FiniteDuration(2, "seconds"))))
        actor ! Start(self, bidTime)
      }
      context.actorSelection("/user/mainActor") ! SellerActive
      context become awaitForAuction

    case StartTest =>
      for(item <- auctionItems) {
        val actor = context.actorOf(Props(new Auction(item, FiniteDuration(2, "seconds"))))
        actor ! Auction.StartTest(self, bidTime)
      }
      context become awaitForAuction
  }

  def awaitForAuction : Actor.Receive = LoggingReceive{
    case Auction.Ignored(from: ActorRef) if timesReList > 0=>
      println("Relisting auction")
      timesReList -= 1
      from ! Start(self, bidTime)
    case Auction.Ignored(_) =>
      println("One of the auction didnt sell, auction didn't sell ")
    case Sold(_, buyer, amount) =>
      println("One of the auction sold, auction sold to ", buyer, " for ", amount)
    case AuctionDeleted(from) if auctionSize == 1 =>
      context.actorSelection("/user/mainActor") ! ActorStopped(self)
      context.stop(self)
    case AuctionDeleted(from) =>
      auctionSize -= 1
  }

}
