package auction.house.remote

import akka.actor.Actor
import akka.event.LoggingReceive
import auction.house.persistent._
import auction.house.remote.Notifier.Notify
import auction.house.remote.NotifierRequest.NotifyAccepted

/**
  * Created by Admin on 2016-12-01.
  */
class AuctionPublisher extends Actor{

  def receive = receiveActivated

  val receiveActivated: Receive = {
    case Notify(currPrice, buyerPath, sellerPath, state) =>
      var info = "Auction info :"
      if(currPrice > 0)info += currPrice.toString + "\n"
      if(buyerPath != "")info += buyerPath + "\n"
      info += sellerPath + "\n"
      info += "State : "
      state match{
        case PreInit =>
          info += "preinit"
        case Created =>
          info += "created"
        case Ignored =>
          info += "ignored"
        case Activated =>
          info += "activated"
        case SoldS =>
          info += "sold"
      }
      info += "\n"
      println(info)

      sender ! NotifyAccepted
  }
}
