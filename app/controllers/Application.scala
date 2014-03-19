package controllers

import play.api.mvc._
import play.api.libs.iteratee.{Iteratee, Concurrent, Enumerator}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import play.api.Logger
import play.api.libs.concurrent.Promise
import java.io.File
import play.api.libs.ws.WS
import scala.concurrent.duration.DurationInt

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

      val inIteratee: Iteratee[String, Unit] = Iteratee.foreach[String](receivedString => {
        // send string back
        Logger.info(s"wsEchoAsync, received: $receivedString")
        channel.foreach(_.push(receivedString))
      })

      (inIteratee, outEnumerator)
    }
  }

  // sends the time every second, ignores any input
  def wsTime = WebSocket.async[String] {
    request => Future {
      Logger.info(s"wsTime, client connected.")

      val outEnumerator: Enumerator[String] = Enumerator.repeatM(Promise.timeout(s"${new java.util.Date()}", 1000))
      val inIteratee: Iteratee[String, Unit] = Iteratee.ignore[String]

      (inIteratee, outEnumerator)
    }
  }

  // sends the time every second, ignores any input
  def wsPingPong = WebSocket.async[String] {
    request => Future {
      Logger.info(s"wsInterleave, client connected.")

      var switch: Boolean = true
      val outEnumerator = Enumerator.repeatM[String](Promise.timeout({
        switch = !switch
        if (switch) "                <----- pong" else "ping ----->"
      }, 1000))

      (Iteratee.ignore[String], outEnumerator)
    }
  }

  // interleaves two enumerators
  def wsInterleave = WebSocket.async[String] {
    request => Future {
      val en1: Enumerator[String] = Enumerator.repeatM(Promise.timeout("AAAA", 2000))
      val en2: Enumerator[String] = Enumerator.repeatM(Promise.timeout("BBBB", 1500))
      (Iteratee.ignore[String], Enumerator.interleave(en1, en2))
    }
  }

  // sends content from a file
  def wsFromFile = WebSocket.async[Array[Byte]] {
    request => Future {
      val file: File = new File("test.txt")
      val outEnumerator = Enumerator.fromFile(file)
      (Iteratee.ignore[Array[Byte]], outEnumerator.andThen(Enumerator.eof))
    }
  }

  // proxies another webservice
  def httpWeatherProxy = Action.async {
    request => {
      val url = "http://api.openweathermap.org/data/2.5/weather?q=Amsterdam,nl"
      WS.url(url).get().map(r => Ok(r.body))
    }
  }

  // proxies another webservice, websocket style
  def wsWeatherProxy = WebSocket.async[String] {
    request => Future {
      val url = "http://api.openweathermap.org/data/2.5/weather?q=Amsterdam,nl"
      var switch = false
      val myEnumerator: Enumerator[String] = Enumerator.generateM[String](WS.url(url).get().map(r => {
        switch = !switch
        if (switch) Option(r.body)
        else None
      }))
      (Iteratee.ignore[String], myEnumerator)
    }
  }

  // watch out! haven't got this working yet!
  def wsWeatherExp = WebSocket.async[String] {
    request => Future {
      val interval = 3 seconds
      val url = "http://api.openweathermap.org/data/2.5/weather?q=Amsterdam,nl"
      val w: Future[String] = WS.url(url).get().map(r => r.body)
      val t = Promise.timeout(w, 5000)
      val myEnumerator = Enumerator.repeatM[String](t)
      (Iteratee.ignore[String], myEnumerator)
    }
  }

}