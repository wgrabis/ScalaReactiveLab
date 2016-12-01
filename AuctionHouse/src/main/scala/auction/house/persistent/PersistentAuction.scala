package auction.house.persistent

import akka.actor.{Actor, ActorRef, Cancellable}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, SnapshotOffer, SnapshotSelectionCriteria}
import auction.house.Auction._
import auction.house.remote.Notifier.Notify
import auction.house.{Auction, AuctionSearch}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.Try
import scala.concurrent.duration._

/**
  * Created by Admin on 2016-11-02.
  */
sealed trait State
case object PreInit extends State
case object Created extends State
case object Ignored extends State
case object Activated extends State
case object SoldS extends State

case class StateChange(state: AuctionState)

case class AuctionState(buyer: ActorRef, currentBid: Int, bidTimer: FiniteDuration, state: State){
  def updatedState(newState: AuctionState): AuctionState ={
    AuctionState(newState.buyer, newState.currentBid, newState.bidTimer, newState.state)
  }

  def updatedTime(timer: FiniteDuration): AuctionState = {
    AuctionState(buyer, currentBid, timer, state)
  }

  override def toString: String = {
    buyer.toString + ":" + currentBid.toString + ":" + bidTimer.toString
  }
}

class PersistentAuction(title: String, deleteTimer: FiniteDuration) extends PersistentActor{
  override def persistenceId = "Auction-" + title

  var state = AuctionState(null, 0, null, PreInit)

  var dispatcher : Cancellable = null

  var startTime : FiniteDuration = currentTime

  def currentTime : FiniteDuration ={
    System.currentTimeMillis() millisecond
  }


  def startTimer(state: State, time: FiniteDuration): Unit ={
    Try(dispatcher.cancel())
    state match {
      case Created => {
        dispatcher = context.system.scheduler.scheduleOnce(time, self, Timeout)
      }
      case Ignored => {
        dispatcher = context.system.scheduler.scheduleOnce(time, self, DeleteTimeout)
      }
      case SoldS => {
        dispatcher = context.system.scheduler.scheduleOnce(time, self, DeleteTimeout)
      }
      case Activated => {
        dispatcher = context.system.scheduler.scheduleOnce(time, self, Timeout)
      }
      case _ =>
    }
  }

  def updateState(oldState: AuctionState): Unit = {
    val newState = oldState.updatedTime(oldState.bidTimer - (currentTime - startTime))
    state = state.updatedState(newState)
    startTimer(newState.state, newState.bidTimer)
    context.become(
      newState.state match{
        case Created => awaitFirstBid
        case Ignored => awaitRelist
        case SoldS => awaitDelete
        case Activated => awaitBids
        case PreInit => waitInit
      }
    )
  }

  val receiveRecover: Receive = {
    case StateChange(other) =>
      updateState(other)
    case SnapshotOffer(_, snapshot: AuctionState) =>
      updateState(snapshot)
  }

  def deleteAuction() = {
      deleteSnapshots(SnapshotSelectionCriteria())
      deleteMessages(Long.MaxValue)
      context.stop(self)
    }

  def serializeActorName(actor: ActorRef): String ={
    akka.serialization.Serialization.serializedActorPath(actor)
  }


  val receiveCommand: Receive = waitInit

  def waitInit : Actor.Receive = LoggingReceive{
    case evt: Start =>
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Register(self, title)
      context.actorSelection("/user/notifier") ! Notify(0, "", serializeActorName(evt.sellerAct), Created)

      startTime = currentTime
      persist(StateChange(AuctionState(null, 0, evt.bidTimer, Created))) {
        event => updateState(event.state)
      }
  }

  def awaitFirstBid : Actor.Receive = LoggingReceive{
    case evt: Bid =>
      Try(state.buyer ! BidChanged(self, evt.amount))

      context.actorSelection("/user/notifier") ! Notify(evt.amount, serializeActorName(evt.from), serializeActorName(context.parent), Activated)

      persist(StateChange(AuctionState(evt.from, evt.amount, state.bidTimer, Activated))) {
        event => updateState(event.state)
      }
    case Timeout =>
      context.parent ! Auction.Ignored(self)

      context.actorSelection("/user/notifier") ! Notify(0, "", serializeActorName(context.parent), Ignored)

      startTime = currentTime
      persist(StateChange(AuctionState(null, 0, deleteTimer, Ignored))) {
        event => updateState(event.state)
      }
    case DeleteTimeout =>
  }

  def awaitBids() : Actor.Receive = LoggingReceive{
    case bid : Bid if bid.amount > state.currentBid =>
      context.actorSelection("/user/notifier") ! Notify(bid.amount, serializeActorName(bid.from), serializeActorName(context.parent), Activated)

      persist(StateChange(AuctionState(bid.from, bid.amount, state.bidTimer, Activated))) {
        event => updateState(event.state)
      }
    case Bid(amount, from) =>
      from ! InvalidBid(amount, self)
    case Timeout =>
      context.parent ! Sold(context.parent, state.buyer, state.currentBid)
      Try(state.buyer ! Sold(context.parent, state.buyer, state.currentBid))

      context.actorSelection("/user/notifier") ! Notify(state.currentBid, serializeActorName(state.buyer), serializeActorName(context.parent), SoldS)

      startTime = currentTime
      persist(StateChange(AuctionState(state.buyer, state.currentBid, deleteTimer, SoldS))) {
        event => updateState(event.state)
      }
    case DeleteTimeout =>
  }

  def awaitRelist() : Actor.Receive = LoggingReceive{
    case start : Start =>

      context.actorSelection("/user/notifier") ! Notify(0, "", serializeActorName(context.parent), Created)

      startTime = currentTime
      persist(StateChange(AuctionState(null, 0, start.bidTimer, Created))) {
        event => updateState(event.state)
      }
    case DeleteTimeout =>
      println("Auction ", title, " no relist ", state.currentBid)
      context.parent ! AuctionDeleted(self)
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Remove(title)
      deleteAuction()
  }

  def awaitDelete() : Actor.Receive = LoggingReceive{
    case DeleteTimeout =>
      println("Auction",title ," stopped, final amount ", state.currentBid)
      context.actorSelection("/user/auctionSearch") ! AuctionSearch.Remove(title)
      context.parent ! AuctionDeleted(self)
      deleteAuction()
  }
}
