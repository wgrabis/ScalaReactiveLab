package auction.house

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import auction.house.Auction.{Ignored, Sold, Start}
import auction.house.AuctionHouse.SellerActive
import auction.house.Seller.{AskForAuction, StartAuction}

import scala.concurrent.duration.FiniteDuration

/**
  * Created by Admin on 2016-10-19.
  */
object Seller{
  case object StartAuction
  case class AskForAuction(from: ActorRef)
}

class Seller(bidTime: FiniteDuration, auction: ActorRef, var timesReList: Int) extends Actor{

  def receive : Actor.Receive= LoggingReceive{
    case StartAuction =>
      auction ! Start(self, bidTime)
      context become awaitForAuction
  }

  def awaitForAuction : Actor.Receive = LoggingReceive{
    case Ignored if timesReList > 0=>
      println("Relisting auction")
      timesReList -= 1
      auction ! Start(self, bidTime)
    case Ignored =>
      println("Stopping seller, auction didn't sell ")
      context.stop(self)
    case Sold(_, buyer, amount) =>
      println("Stopping seller, auction sold to ", buyer, " for ", amount)
      context.stop(self)
  }

}
