import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestActors, TestProbe}
import auction.house.{Auction, AuctionSearch}
import auction.house.Auction.{Bid, InvalidBid, Sold, Start}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.duration._

/**
  * Created by wgrabis on 01.11.2016.
  */
class AuctionTest extends FlatSpec with BeforeAndAfterAll{
  implicit val system = ActorSystem("Reactive2")
  val auctionSearchProbe = TestProbe()


  override def beforeAll() ={
    system.actorOf(Props(new ForwardingActor(auctionSearchProbe.ref)), "auctionSearch")
  }

  "Auction" should "register" in{
    val sellerProbe = TestProbe()

    val auction = system.actorOf(Props(new  Auction("test1", 10 seconds)))

    auction ! Start(sellerProbe.ref, 1 seconds)
    auctionSearchProbe.expectMsgPF(){
      case AuctionSearch.Register(_, "test1") => ()
    }
  }

  "Auction" should "accept Bid" in{

    val sellerProbe = TestProbe()
    val buyerProbe = TestProbe()
    val auction = system.actorOf(Props(new  Auction("test2", 10 seconds)))

    auction ! Start(sellerProbe.ref, 1 seconds)
    auctionSearchProbe.expectMsgPF(){
      case AuctionSearch.Register(_, "test2") => ()
    }

    auction ! Bid(1, buyerProbe.ref)
    buyerProbe.expectMsgPF() {
      case Sold(_, _, 1) => ()
    }
  }

  "Auction" should "not accept Bid" in{
    val sellerProbe = TestProbe()
    val buyerProbe = TestProbe()
    val otherProbe = TestProbe()

    val auction = system.actorOf(Props(new  Auction("test3", 10 seconds)))

    auction ! Start(sellerProbe.ref, 1 seconds)
    auctionSearchProbe.expectMsgPF(){
      case AuctionSearch.Register(_, "test3") => ()
    }

    auction ! Bid(10, buyerProbe.ref)

    auction ! Bid(1, otherProbe.ref)
    otherProbe.expectMsgPF(){
      case InvalidBid(_, _) =>()
    }

    buyerProbe.expectMsgPF() {
      case Sold(_, _, 10) => ()
    }
  }

  "Auction" should "ignore" in{
    val sellerProbe = TestProbe()

    val auction = system.actorOf(Props(new  Auction("test4", 10 seconds)))

    auction ! Start(sellerProbe.ref, 1 seconds)
    auctionSearchProbe.expectMsgPF(){
      case AuctionSearch.Register(_, "test4") => ()
    }

    sellerProbe.expectMsgPF(){
      case Auction.Ignored(_) =>()
    }
  }

}
