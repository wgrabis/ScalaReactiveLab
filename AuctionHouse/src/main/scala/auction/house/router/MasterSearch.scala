package auction.house.router

import akka.actor.{Actor, Props, Terminated}
import akka.routing._
import auction.house.AuctionSearch
import auction.house.AuctionSearch.{RegisterWork, RequestWork}
import scala.concurrent.duration._
import scala.concurrent._

/**
  * Created by Admin on 2016-11-30.
  */

object MasterSearch {

}

class MasterSearch(searchNumber : Int) extends Actor{
  var responseRouter: Router = null
  var registerRouter: Router = null

  {
    val routees = Vector.fill(searchNumber) {
      val r = context.actorOf(Props[AuctionSearch])
      context watch r
      ActorRefRoutee(r)
    }

    responseRouter = Router(RoundRobinRoutingLogic(), routees)
    registerRouter = Router(BroadcastRoutingLogic(), routees)
  }

  def receive = {
    case w: RequestWork =>
      responseRouter.route(w, sender())
    case w: RegisterWork =>
      registerRouter.route(w, sender())
    case Terminated(a) =>
      responseRouter = responseRouter.removeRoutee(a)
      registerRouter = registerRouter.removeRoutee(a)

      val r = context.actorOf(Props[AuctionSearch])
      context watch r

      responseRouter = responseRouter.addRoutee(r)
      registerRouter = registerRouter.addRoutee(r)
  }
}
