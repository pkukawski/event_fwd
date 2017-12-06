package pl.qki

package object model {

  case class NetworkElement(id: Int, interface: String, port: Int)

  case class Monotype(atr: String, v: Int)
  case class Pm(timestamp: Long, status: String, vals: List[Monotype])

  class CannotConnectException extends RuntimeException

}
