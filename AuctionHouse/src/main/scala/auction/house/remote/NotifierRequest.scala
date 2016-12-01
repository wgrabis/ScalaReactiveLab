package auction.house.remote

import akka.actor.{Actor, ActorRef, ActorSelection, Identify}
import akka.event.LoggingReceive
import auction.house.remote.Notifier.Notify
import auction.house.remote.NotifierRequest.{NotifierTimeout, NotifyAccepted, NotifyException}

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by Admin on 2016-12-01.
  */

object NotifierRequest{
  class  NotifyException(message: String) extends Throwable{
    override def toString: String = message
  }

  case object NotifierTimeout
  case object NotifyAccepted
}

class NotifierRequest extends Actor{

  var auctionPublisher: ActorSelection = null

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    auctionPublisher = context.actorSelection("akka.tcp://Publisher@127.0.0.1:2553/user/auctionPublisher")
    auctionPublisher ! Identify(self)
  }

  def receive =  awaitInit

  val awaitInit: Receive = {
    case notify: Notify =>
      context.system.scheduler.scheduleOnce(10 seconds, self, NotifierTimeout)
      auctionPublisher ! notify
    case NotifierTimeout =>
      throw new NotifyException("Publisher unavailable")
  }

  val awaitResponse: Receive = {
    case NotifierTimeout =>
      throw new NotifyException("Publishing error")

    case NotifyAccepted =>
      context.stop(self)
  }
}
