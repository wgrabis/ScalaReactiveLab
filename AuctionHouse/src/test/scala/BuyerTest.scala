import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import auction.house.Auction.Bid
import auction.house.Buyer.Init
import auction.house.{AuctionSearch, Buyer, Seller}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.duration.FiniteDuration

/**
  * Created by wgrabis on 01.11.2016.
  */
class BuyerTest extends FlatSpec with BeforeAndAfterAll{
  implicit val system = ActorSystem("Reactive2")
  val auctionSearchProbe = TestProbe()
  val auctionHouseProbe = TestProbe()


  override def beforeAll() ={
    system.actorOf(Props(new ForwardingActor(auctionSearchProbe.ref)), "auctionSearch")
  }


  "a buyer" should "find" in{
    val buyer = system.actorOf(Props(new Buyer(auctionHouseProbe.ref, 10, "test")))
    val testAuction = TestProbe()

    buyer ! Init
    auctionSearchProbe.expectMsgPF(){
      case AuctionSearch.Search(ref, "test") =>
        ref ! AuctionSearch.ResponseMultiple(Array(testAuction.ref))
        ()
    }

    testAuction.expectMsgPF() {
      case Bid(_, _) =>
        ()
    }
  }

  "a buyer" should "not find" in{
    val buyer = system.actorOf(Props(new Buyer(auctionHouseProbe.ref, 10, "test")))

    buyer ! Init
    auctionSearchProbe.expectMsgPF(){
      case AuctionSearch.Search(ref, "test") =>
        ref ! AuctionSearch.NotFound
        ()
    }

    auctionSearchProbe.expectMsgPF(){
      case AuctionSearch.Search(ref, "test") =>
        ()
    }
  }
}
