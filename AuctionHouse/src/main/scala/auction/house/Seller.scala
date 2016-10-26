package auction.house

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import auction.house.Auction.{AuctionDeleted, Sold, Start}
import auction.house.AuctionHouse.{ActorStopped, SellerActive}
import auction.house.Seller.{AskForAuction, StartAuction}

import scala.concurrent.duration.FiniteDuration

/**
  * Created by Admin on 2016-10-19.
  */
object Seller{
  case object StartAuction
  case class AskForAuction(from: ActorRef)
}

class Seller(bidTime: FiniteDuration, var timesReList: Int) extends Actor{

  def receive : Actor.Receive= LoggingReceive{
    case StartAuction =>
      val actor = context.actorOf(Props(new Auction("test", FiniteDuration(2, "seconds"))))
      actor ! Start(self, bidTime)
      context.actorSelection("/user/mainActor") ! SellerActive
      context become awaitForAuction
  }

  def awaitForAuction : Actor.Receive = LoggingReceive{
    case Auction.Ignored(from: ActorRef) if timesReList > 0=>
      println("Relisting auction")
      timesReList -= 1
      from ! Start(self, bidTime)
    case Auction.Ignored(_) =>
      println("Stopping seller, auction didn't sell ")
    case Sold(_, buyer, amount) =>
      println("Stopping seller, auction sold to ", buyer, " for ", amount)
    case AuctionDeleted(from) =>
      context.actorSelection("/user/mainActor") ! ActorStopped(self)
      context.stop(self)
  }

}
