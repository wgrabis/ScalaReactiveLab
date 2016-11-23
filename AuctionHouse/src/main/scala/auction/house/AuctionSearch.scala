package auction.house

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import auction.house.AuctionSearch.{NotFound, ResponseMultiple, ResponseSingle}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * Created by Admin on 2016-10-25.
  */

object AuctionSearch{
  case class Register(from: ActorRef, title: String)
  case class Remove(title: String)
  case class Search(from: ActorRef, request: String)
  case class Get(from: ActorRef, request: String)

  case class ResponseSingle(auction: ActorRef)
  case class ResponseMultiple(auctions: Array[ActorRef])
  case object NotFound
}

class AuctionSearch extends Actor{

  val cache = collection.mutable.Map[String, ActorRef]()

  def find(request: String) : Array[ActorRef] = {
    var result = ArrayBuffer.empty[ActorRef]

    cache.foreach{
      case (key, value) if key contains request =>
        result += value
      case _ =>
    }

    if(result.nonEmpty)
      result.toArray
    else
      null
  }


  def receive = LoggingReceive{
    case AuctionSearch.Register(from, title) =>
      cache(title) = from
    case AuctionSearch.Remove(title) =>
      cache -= title
    case AuctionSearch.Get(from, request) =>
      cache get request match{
        case None =>
          from ! NotFound
        case Some(auction) =>
          from ! ResponseSingle(auction)
      }
    case AuctionSearch.Search(from, request) =>
      val auctions = find(request)
      Option(auctions) match {
        case Some(a) =>
          from ! ResponseMultiple(a)
        case None =>
          from ! NotFound
      }
  }
}
