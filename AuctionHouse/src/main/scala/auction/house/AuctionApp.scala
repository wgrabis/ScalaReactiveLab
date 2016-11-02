package auction.house

import java.util

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import auction.house.AuctionHouse.{ActorStopped, SellerActive}
import auction.house.Seller.{StartAuction, StartAuctionFSM}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Created by Admin on 2016-10-19.
  */

object AuctionHouse{
  case object Init
  case object InitFSM
  case object SellerActive
  case class ActorStopped(actorRef: ActorRef)
}

class AuctionHouse(var noSellers: Int, var noBuyers: Int) extends Actor{
  var sellers : Array[ActorRef] = new Array[ActorRef](noSellers)
  var buyers : Array[ActorRef] = new Array[ActorRef](noBuyers)

  var inactiveSellers = noSellers

  def init() = {
    for (i <- 0 until noSellers) {
      var items = ListBuffer.empty[String]
      if(i == 0)items += "super car"
      if(i == 1)items += "old car"
      if(i == 2)items += "very old car"
      if(i == 1)items += "classic painting"
      if(i == 2)items += "antique painting"
      sellers(i) = context.actorOf(Props(new Seller(FiniteDuration(2, "seconds"), 2, items.toList)))
    }

    for (i <- 0  until noBuyers) {
      if(i < 3)buyers(i) = context.actorOf(Props(new Buyer(self ,10, "car")))
      if(i >= 3)buyers(i) = context.actorOf(Props(new Buyer(self ,10, "painting")))
    }
  }

  def receive = LoggingReceive{
    case AuctionHouse.Init =>
      init()
      for (i <- 0 until noSellers)
        sellers(i) ! StartAuctionFSM
    case SellerActive if inactiveSellers == 1 =>
      for (i <- 0  until noBuyers)
        buyers(i) ! Buyer.Init
    case SellerActive =>
      inactiveSellers -= 1
    case ActorStopped(_) if noSellers == 0 && noBuyers == 1 =>
      print("All auctions stopped, turning off")
      context.system.terminate
    case ActorStopped(from) if noSellers > 0=>
      noSellers -= 1
    case ActorStopped(_) =>
      noBuyers -= 1
  }
}


object AuctionApp extends App {
  val system = ActorSystem("Reactive2")
  val mainActor = system.actorOf(Props(new AuctionHouse(3, 5)), "mainActor")
  val auctionSearch = system.actorOf(Props(new AuctionSearch), "auctionSearch")

  mainActor ! AuctionHouse.Init

  Await.result(system.whenTerminated, Duration.Inf)
}
