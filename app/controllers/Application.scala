package controllers

import play.api.mvc._
import play.api.libs.iteratee.{Iteratee, Concurrent, Enumerator}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger
import play.api.libs.concurrent.Promise

object Application extends Controller {

  // http endpoint to check that the server is running
  def index = Action {
    Ok("I'm alive!\n")
  }

  // endpoint that opens an echo websocket
  def wsEcho = WebSocket.using[String] {
    request => {
      Logger.info(s"wsEcho, client connected.")
      var channel: Option[Concurrent.Channel[String]] = None
      val outEnumerator: Enumerator[String] = Concurrent.unicast(c => channel = Some(c))

      // Log events to the console
      val inIteratee: Iteratee[String, Unit] = Iteratee.foreach[String](receivedString => {
        // send string back
        Logger.info(s"wsEcho, received: $receivedString")
        channel.foreach(_.push(receivedString))
      })

      (inIteratee, outEnumerator)
    }
  }

  // async version of the echo websocket -- code is exactly the same!
  def wsEchoAsync = WebSocket.async[String] {
    request => Future {
      Logger.info(s"wsEchoAsync, client connected.")
      var channel: Option[Concurrent.Channel[String]] = None
      val outEnumerator: Enumerator[String] = Concurrent.unicast(c => channel = Some(c))

      // Log events to the console
      val inIteratee: Iteratee[String, Unit] = Iteratee.foreach[String](receivedString => {
        // send string back
        Logger.info(s"wsEchoAsync, received: $receivedString")
        channel.foreach(_.push(receivedString))
      })

      (inIteratee, outEnumerator)
    }
  }

  // sends the time every second, ignores any input
  def wsTimeAsync = WebSocket.async[String] {
    request => Future {
      Logger.info(s"wsTimeAsync, client connected.")

      val outEnumerator: Enumerator[String] = Enumerator.generateM(Promise.timeout(Some(s"${new java.util.Date()}"), 1000))

      // Log events to the console
      val inIteratee: Iteratee[String, Unit] = Iteratee.ignore[String]

      (inIteratee, outEnumerator)
    }
  }

}