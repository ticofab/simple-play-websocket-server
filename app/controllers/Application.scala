package controllers

import java.io.File
import javax.inject.Inject

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import play.api.Play.current
import play.api._
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.ws._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject()(ws: WSClient) extends Controller {

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

  // sends the time every second, ignores any input
  def wsTime = WebSocket.using[String] {
    request =>
      Logger.info(s"wsTime, client connected.")

      val outEnumerator: Enumerator[String] = Enumerator.repeatM(Promise.timeout(s"${new java.util.Date()}", 1000))
      val inIteratee: Iteratee[String, Unit] = Iteratee.ignore[String]

      (inIteratee, outEnumerator)
  }

  // sends the time every second, ignores any input
  def wsPingPong = WebSocket.using[String] {
    request =>
      Logger.info(s"wsPingPong, client connected.")

      var switch: Boolean = true
      val outEnumerator = Enumerator.repeatM[String](Promise.timeout({
        switch = !switch
        if (switch) "                <----- pong" else "ping ----->"
      }, 1000))

      (Iteratee.ignore[String], outEnumerator)
  }

  // interleaves two enumerators
  def wsInterleave = WebSocket.using[String] {
    request =>
      Logger.info("wsInterleave, client connected")
      val en1: Enumerator[String] = Enumerator.repeatM(Promise.timeout("AAAA", 2000))
      val en2: Enumerator[String] = Enumerator.repeatM(Promise.timeout("BBBB", 1500))
      (Iteratee.ignore[String], Enumerator.interleave(en1, en2))
  }

  // sends content from a file
  def wsFromFile = WebSocket.using[Array[Byte]] {
    request =>
      Logger.info("wsFromFile, client connected")
      val file: File = new File("test.txt")
      val outEnumerator = Enumerator.fromFile(file)
      (Iteratee.ignore[Array[Byte]], outEnumerator.andThen(Enumerator.eof))
  }

  object EchoWebSocketActor {
    def props(out: ActorRef) = Props(new EchoWebSocketActor(out))
  }

  class EchoWebSocketActor(out: ActorRef) extends Actor {
    def receive = {
      case msg: String =>
        Logger.info(s"actor, received message: $msg")
        if (msg == "goodbye") self ! PoisonPill
        else out ! ("I received your message: " + msg)
    }
  }

  def wsWithActor = WebSocket.acceptWithActor[String, String] {
    request =>
      out => {
        Logger.info("wsWithActor, client connected")
        EchoWebSocketActor.props(out)
      }
  }

  // proxies another webservice
  def httpWeatherProxy = Action.async {
    request => {
      Logger.info("httpWeatherProxy, client connected")
      val url = "http://api.openweathermap.org/data/2.5/weather?q=Amsterdam,nl"
      ws.url(url).get().map(r => Ok(r.body))
    }
  }

  // proxies another webservice, websocket style
  def wsWeatherProxy = WebSocket.using[String] {
    request =>
      Logger.info("wsWeatherProxy, client connected")
      val url = "http://api.openweathermap.org/data/2.5/weather?q=Amsterdam,nl"
      var switch = false
      val myEnumerator: Enumerator[String] = Enumerator.generateM[String](ws.url(url).get().map(r => {
        switch = !switch
        if (switch) Option(r.body)
        else None
      }))
      (Iteratee.ignore[String], myEnumerator)
  }

  // proxies another webservice at regular intervals
  def wsWeatherIntervals = WebSocket.using[String] {
    request =>
      Logger.info("wsWeatherIntervals, client connected")
      val url = "http://api.openweathermap.org/data/2.5/weather?q=Amsterdam,nl"
      val outEnumerator = Enumerator.repeatM[String]({
        Thread.sleep(3000)
        ws.url(url).get().map(r => s"${new java.util.Date()}\n ${r.body}")
      })

      (Iteratee.ignore[String], outEnumerator)
  }
}
