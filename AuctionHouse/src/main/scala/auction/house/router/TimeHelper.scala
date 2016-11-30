package auction.house.router

/**
  * Created by Admin on 2016-11-30.
  */
class TimeHelper {
  var t0: Long = 0

  def startMeasuer(): Unit = {
    t0 = System.currentTimeMillis()
  }

  def finishMeasue(): Long = {
    val result = System.currentTimeMillis() - t0
    result
  }
}
