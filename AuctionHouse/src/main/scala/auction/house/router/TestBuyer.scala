package auction.house.router

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import auction.house.Auction.{Bid, BidChanged, InvalidBid, Sold}
import auction.house.AuctionHouse.{ActorStopped, TestShutdown}
import auction.house.AuctionSearch
import auction.house.Buyer.{FindNewAuction, Init}

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
  * Created by Admin on 2016-11-30.
  */
class TestBuyer(auctionHouse: ActorRef, auctionToBid: String, var spamNumber: Int) extends Actor{

  def receive = LoggingReceive{
    case Init =>
      self ! FindNewAuction
      context become searchingAuction
  }

  def searchingAuction : Actor.Receive = LoggingReceive{
    case FindNewAuction =>
      var spam = spamNumber
      val father = context.actorSelection("/user/auctionSearch")
      while(spam > 0) {
         father ! AuctionSearch.Search(self, auctionToBid)
        spam -= 1
      }


    case AuctionSearch.ResponseMultiple(auctions) if spamNumber == 1 =>
      auctionHouse ! TestShutdown
      context.stop(self)
    case AuctionSearch.ResponseMultiple(auctions) =>
      spamNumber -= 1
    case AuctionSearch.NotFound if spamNumber == 1=>
      auctionHouse ! TestShutdown
      context.stop(self)
    case AuctionSearch.NotFound =>
      spamNumber -= 1
  }
}
