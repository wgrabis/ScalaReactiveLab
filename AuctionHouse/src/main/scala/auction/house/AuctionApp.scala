package auction.house

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import auction.house.AuctionHouse.ActorStopped
import auction.house.Seller.StartAuction

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Created by Admin on 2016-10-19.
  */

object AuctionHouse{
  case object Init
  case class ActorStopped(actorRef: ActorRef)
}

class AuctionHouse(var noSellers: Int, var noBuyers: Int) extends Actor{
  var sellers : Array[ActorRef] = new Array[ActorRef](noSellers)
  var buyers : Array[ActorRef] = new Array[ActorRef](noBuyers)
  var auctions : Array[ActorRef] = new Array[ActorRef](noSellers)

  def init() = {
    for (i <- 0 until noSellers) {
      auctions(i) = context.actorOf(Props(new Auction(self ,FiniteDuration(2, "seconds"))))
      sellers(i) = context.actorOf(Props(new Seller(FiniteDuration(2, "seconds"), auctions(i), 2)))
    }

    for (i <- 0  until noBuyers) {
      buyers(i) = context.actorOf(Props(new Buyer(self ,10, auctions)))
    }
  }

  def receive = LoggingReceive{
    case AuctionHouse.Init =>
      init()
      for (i <- 0 until noSellers)
        sellers(i) ! StartAuction
      for (i <- 0  until noBuyers)
        buyers(i) ! Buyer.Init
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
  val mainActor = system.actorOf(Props(new AuctionHouse(10, 5)), "mainActor")

  mainActor ! AuctionHouse.Init

  Await.result(system.whenTerminated, Duration.Inf)
}
