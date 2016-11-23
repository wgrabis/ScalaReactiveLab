import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActors, TestProbe}
import auction.house.Seller
import org.scalatest.FlatSpec

import scala.concurrent.duration.FiniteDuration

/**
  * Created by wgrabis on 01.11.2016.
  */
class SellerTest extends FlatSpec{
  implicit val system = ActorSystem("Reactive2")

  "A seller" should "create auction" in{
    val auctionSearchProbe = TestProbe()
    val seller = system.actorOf(Props(new Seller(FiniteDuration(2, "seconds"), 0, List("test"))))

    seller ! Seller.StartAuction
    val echo = system.actorOf(TestActors.echoActorProps, "auctionSearch")
  }
}
