import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive

/**
  * Created by Admin on 2016-11-03.
  */
class ForwardingActor(forward: ActorRef) extends Actor{
  def receive : Actor.Receive = LoggingReceive{
    case something =>
      forward ! something
  }
}
