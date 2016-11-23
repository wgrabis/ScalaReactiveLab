import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestProbe
import auction.house.AuctionSearch
import org.scalatest.FlatSpec

import scala.concurrent.duration._
import scala.concurrent.Future

/**
  * Created by wgrabis on 01.11.2016.
  */
class AuctionSearchTest extends FlatSpec{
  implicit val system = ActorSystem("Reactive2")

  "An empty auctionSearch" should "not find" in{
    val auctionSearch = system.actorOf(Props[AuctionSearch])
    val sellerProbe = TestProbe()

    auctionSearch ! AuctionSearch.Get(sellerProbe.ref, "test")
    sellerProbe.expectMsg(AuctionSearch.NotFound)

    auctionSearch ! AuctionSearch.Search(sellerProbe.ref, "test")
    sellerProbe.expectMsg(AuctionSearch.NotFound)

    val auctionProbe = TestProbe()

    auctionSearch ! AuctionSearch.Register(auctionProbe.ref, "not")

    auctionSearch ! AuctionSearch.Get(sellerProbe.ref, "test")
    sellerProbe.expectMsg(AuctionSearch.NotFound)

    auctionSearch ! AuctionSearch.Search(sellerProbe.ref, "test")
    sellerProbe.expectMsg(AuctionSearch.NotFound)

    auctionSearch ! AuctionSearch.Remove("not")

    auctionSearch ! AuctionSearch.Get(sellerProbe.ref, "not")
    sellerProbe.expectMsg(AuctionSearch.NotFound)

    auctionSearch ! AuctionSearch.Search(sellerProbe.ref, "not")
    sellerProbe.expectMsg(AuctionSearch.NotFound)
  }

  "An non empty auctionSearch" should "find" in{
    val auctionSearch = system.actorOf(Props[AuctionSearch])
    val sellerProbe = TestProbe()
    val auctionProbe = TestProbe()

    auctionSearch ! AuctionSearch.Register(auctionProbe.ref, "test")

    auctionSearch ! AuctionSearch.Get(sellerProbe.ref, "test")
    sellerProbe.expectMsg(AuctionSearch.ResponseSingle(auctionProbe.ref))

    auctionSearch ! AuctionSearch.Search(sellerProbe.ref, "test")
    sellerProbe.expectMsgPF() {
      case AuctionSearch.ResponseMultiple(auctions: Array[ActorRef]) =>
        auctions match{
          case Array(auction) if auction == auctionProbe.ref => ()
        }
    }

  }
}
