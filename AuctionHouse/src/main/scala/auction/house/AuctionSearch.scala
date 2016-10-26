package auction.house

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import auction.house.AuctionSearch.{NotFound, Response}

/**
  * Created by Admin on 2016-10-25.
  */

object AuctionSearch{
  case class Register(from: ActorRef, title: String)
  case class Remove(title: String)
  case class Search(from: ActorRef, request: String)

  case class Response(auction: ActorRef)
  case object NotFound
}

class AuctionSearch extends Actor{

  val cache = collection.mutable.Map[String, ActorRef]()

  def receive = LoggingReceive{
    case AuctionSearch.Register(from, title) =>
      cache(title) = from
    case AuctionSearch.Remove(title) =>
      cache -= title
    case AuctionSearch.Search(from, request) =>
      cache get request match{
        case None =>
          from ! NotFound
        case Some(auction) =>
          from ! Response(auction)
      }
  }
}
