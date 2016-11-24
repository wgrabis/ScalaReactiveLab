import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActors, TestProbe}
import auction.house.Auction.AuctionDeleted
import auction.house.AuctionHouse.SellerActive
import auction.house.{AuctionSearch, Seller}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.duration.FiniteDuration

/**
  * Created by wgrabis on 01.11.2016.
  */
class SellerTest extends FlatSpec with BeforeAndAfterAll{
  implicit val system = ActorSystem("Reactive2")
  val auctionSearchProbe = TestProbe()
  val mainActor = TestProbe()


  override def beforeAll() ={
    system.actorOf(Props(new ForwardingActor(auctionSearchProbe.ref)), "auctionSearch")
    system.actorOf(Props(new ForwardingActor(mainActor.ref)), "mainActor")
  }

  "A seller" should "create auction" in{
    val seller = system.actorOf(Props(new Seller(FiniteDuration(2, "seconds"), 0, List("test"))))

    seller ! Seller.StartAuction
    mainActor.expectMsg(SellerActive)

    system.stop(seller)
  }

  "A seller" should "create auctionFSM" in{
    val seller = system.actorOf(Props(new Seller(FiniteDuration(2, "seconds"), 0, List("testfsm"))))

    seller ! Seller.StartAuctionFSM
    mainActor.expectMsg(SellerActive)

    system.stop(seller)
  }

  "A seller" should "create auctionPersist" in{
    val seller = system.actorOf(Props(new Seller(FiniteDuration(2, "seconds"), 0, List("testpersi"))))

    seller ! Seller.StartPersist
    mainActor.expectMsg(SellerActive)

    system.stop(seller)
  }
}
