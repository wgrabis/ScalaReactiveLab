package auction.house

import java.util

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import auction.house.AuctionHouse.{ActorStopped, SellerActive, TestShutdown}
import auction.house.Seller.{StartAuction, StartAuctionFSM, StartPersist, StartTest}
import auction.house.remote.{AuctionPublisher, Notifier}
import auction.house.router.{MasterSearch, TestBuyer, TimeHelper}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent._

/**
  * Created by Admin on 2016-10-19.
  */

object AuctionHouse{
  case object Init
  case object InitFSM
  case object InitPersist
  case object SellerActive
  case object InitSpecial
  case class ActorStopped(actorRef: ActorRef)
  case object TestShutdown
}

class AuctionHouse(var noSellers: Int, var noBuyers: Int) extends Actor{
  var sellers : Array[ActorRef] = new Array[ActorRef](noSellers)
  var buyers : Array[ActorRef] = new Array[ActorRef](noBuyers)

  var inactiveSellers = noSellers

  val timeHelper = new TimeHelper()

  def init() = {
    for (i <- 0 until noSellers) {
      var items = ListBuffer.empty[String]
      if(i == 0)items += "super car"
      if(i == 1)items += "old car"
      if(i == 2)items += "very old car"
      if(i == 1)items += "classic painting"
      if(i == 2)items += "antique painting"
      sellers(i) = context.actorOf(Props(new Seller(5 seconds, 2, items.toList)))
    }

    for (i <- 0  until noBuyers) {
      if(i < 3)buyers(i) = context.actorOf(Props(new Buyer(self ,10, "car")))
      if(i >= 3)buyers(i) = context.actorOf(Props(new Buyer(self ,10, "painting")))
    }
  }

  def initSpecialTest() = {
    var items = ListBuffer.empty[String]
    val testAuction = "test item"
    for (i <- 0 until 50000){
      var item = ""
      for(x <- 0 until scala.util.Random.nextInt(50)){
        item = item + " test"
      }
      items += item
    }

    sellers(0) = context.actorOf(Props(new Seller(60 seconds, 1, items.toList)))
    buyers(0) = context.actorOf(Props(new TestBuyer(self, testAuction, 1000000)))
  }

  def receive = LoggingReceive{
    case AuctionHouse.InitSpecial =>
      initSpecialTest()
      sellers(0) ! StartTest
    case AuctionHouse.Init =>
      init()
      for (i <- 0 until noSellers)
        sellers(i) ! StartAuction
    case AuctionHouse.InitFSM =>
      init()
      for (i <- 0 until noSellers)
        sellers(i) ! StartAuctionFSM
    case AuctionHouse.InitPersist =>
      init()
      for (i <- 0 until noSellers)
        sellers(i) ! StartPersist
    case SellerActive if inactiveSellers == 1 =>
      timeHelper.startMeasuer()
      for (i <- 0  until noBuyers)
        buyers(i) ! Buyer.Init
    case SellerActive =>
      inactiveSellers -= 1
    case ActorStopped(_) if noSellers == 1=>
      print("All auctions stopped, turning off")
      context.system.terminate
    case ActorStopped(from) if noSellers > 0=>
      noSellers -= 1
    case TestShutdown =>
      val time = timeHelper.finishMeasue()
      println("Requested test shutdown, turning off")
      println("Time taken:")
      println(time + " ns")
      println("Terminating...")
      context.system.terminate
  }
}


object AuctionApp extends App {
  val configFactory = ConfigFactory.load()
  val mainSystem = ActorSystem("Reactive2", configFactory.getConfig("auctions").withFallback(configFactory))
  val mainActor = mainSystem.actorOf(Props(new AuctionHouse(5, 5)), "mainActor")
  val auctionSearch = mainSystem.actorOf(Props(new MasterSearch(4)), "auctionSearch")


  val publisherSystem = ActorSystem("Publisher", configFactory.getConfig("publisher").withFallback(configFactory))
  val publisher = publisherSystem.actorOf(Props(new AuctionPublisher), "auctionPublisher")

  val notifier = mainSystem.actorOf(Props(new Notifier()), "notifier")

  mainActor ! AuctionHouse.InitPersist

  Await.result(mainSystem.whenTerminated, Duration.Inf)
  Await.result(publisherSystem.whenTerminated, Duration.Inf)
}
