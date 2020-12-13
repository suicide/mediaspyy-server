package mediaspyy

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

import zio.interop.catz._
import zio.Task

trait ApiRoutes {

  lazy val dsl: Http4sDsl[Task] = Http4sDsl[Task]
  import dsl._

  def routes = HttpRoutes.of[Task]{
    case GET -> Root / "hello" => Ok("world")
  }
}
