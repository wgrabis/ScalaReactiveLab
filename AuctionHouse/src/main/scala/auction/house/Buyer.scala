package auction.house

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import auction.house.Auction.{Bid, BidChanged, InvalidBid, Sold}
import auction.house.AuctionHouse.ActorStopped
import auction.house.Buyer.{FindNewAuction, Init}

/**
 * Created by Admin on 2016-10-19.
 */
object Buyer{
  case object Init
  case object FindNewAuction
}

class Buyer(auctionHouse: ActorRef, cash: Int, auctions: Array[ActorRef]) extends Actor {

  require(cash > 0)

  def receive = LoggingReceive{
    case Init =>
      self ! FindNewAuction
      context become startBidding
  }

  def startBidding = LoggingReceive{
    case FindNewAuction =>
      val ind = scala.util.Random.nextInt(auctions.length)
      auctions(ind) ! Bid(scala.util.Random.nextInt(cash - 1) + 1, self)

    case InvalidBid(amount, auction) if cash > amount =>
      auction ! Bid(amount + 1, self)
    case InvalidBid(amount, auction) =>
      self ! FindNewAuction

    case BidChanged(auction, amount) if cash > amount =>
      auction ! Bid(amount + 1, self)
    case BidChanged(auction, amount) =>
      self ! FindNewAuction

    case Sold(seller, _, currentBid) =>
      println("Seller bought item from ", seller, " for ", currentBid)
      auctionHouse ! ActorStopped(self)
      context.stop(self)
  }
}