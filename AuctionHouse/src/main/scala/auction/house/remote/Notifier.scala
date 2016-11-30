package auction.house.remote

import akka.actor.Actor
import akka.event.LoggingReceive
import auction.house.remote.Notifier.Notify

/**
  * Created by Admin on 2016-11-30.
  */

object Notifier{
  case class Notify(currPrice: Int, buyerPath: String, sellerPath: String)
}

class Notifier(remotePath: String) extends Actor{

  def receive = LoggingReceive{
    case Notify(currPrice, buyerPath, sellerPath) =>
      val auctionPublisher = context.actorSelection("akka.tcp://" + remotePath)
      auctionPublisher ! Notify(0,null,null)
  }
}
