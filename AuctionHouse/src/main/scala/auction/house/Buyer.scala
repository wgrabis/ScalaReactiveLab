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

class Buyer(auctionHouse: ActorRef, cash: Int, auctionToBid: String) extends Actor {

  require(cash > 0)

  def receive = LoggingReceive{
    case Init =>
      self ! FindNewAuction
      context become searchingAuction
  }

  def searchingAuction : Actor.Receive = LoggingReceive{
    case FindNewAuction =>
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Search(self, auctionToBid)

    case AuctionSearch.ResponseMultiple(auctions) if auctions.length == 0 =>
      self ! FindNewAuction
    case AuctionSearch.ResponseMultiple(auctions) =>
      val ind = scala.util.Random.nextInt(auctions.length)
      auctions(ind) ! Bid(scala.util.Random.nextInt((cash - 1)/2) + 1, self)
      context become startBidding
    case AuctionSearch.NotFound =>
      self ! FindNewAuction
  }

  def startBidding : Actor.Receive = LoggingReceive{
    case InvalidBid(amount, auction) if cash > amount =>
      auction ! Bid(amount + 1, self)
    case InvalidBid(amount, auction) =>
      self ! FindNewAuction
      context become searchingAuction

    case BidChanged(auction, amount) if cash > amount =>
      auction ! Bid(amount + 1, self)
    case BidChanged(auction, amount) =>
      self ! FindNewAuction
      context become searchingAuction

    case Sold(seller, _, currentBid) =>
      println("Buyer bought item from ", seller, " for ", currentBid)
      auctionHouse ! ActorStopped(self)
      context.stop(self)
  }
}
