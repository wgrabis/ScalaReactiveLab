package auction.house.remote

import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{Actor, Identify, OneForOneStrategy, Props}
import akka.event.LoggingReceive
import auction.house.persistent.State
import auction.house.remote.Notifier.Notify
import auction.house.remote.NotifierRequest.{NotifyException}


/**
  * Created by Admin on 2016-11-30.
  */

object Notifier{
  case class Notify(currPrice: Int, buyerPath: String, sellerPath: String, state: State)
}

class Notifier() extends Actor{


  override val supervisorStrategy = OneForOneStrategy(loggingEnabled = false) {
    case e: NotifyException =>
      println("Error while publishing : " + e.getMessage)
      Stop
    case _ =>
      Escalate
  }

  def receive = LoggingReceive{
    case notify: Notify =>

      val request = context.actorOf(Props(new NotifierRequest))
      request ! notify
  }
}
