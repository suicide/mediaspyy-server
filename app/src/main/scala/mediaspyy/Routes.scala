package mediaspyy

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

import zio.interop.catz._
import zio.Task

import io.circe.generic.auto._
import io.circe.Decoder
import io.circe.Encoder
import org.http4s.circe._
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder

trait ApiRoutes {

  implicit def circeJsonDecoder[A](implicit
      decoder: Decoder[A]
  ): EntityDecoder[Task, A] = jsonOf[Task, A]
  implicit def circeJsonEncoder[A](implicit
      decoder: Encoder[A]
  ): EntityEncoder[Task, A] = jsonEncoderOf[Task, A]

  lazy val dsl: Http4sDsl[Task] = Http4sDsl[Task]
  import dsl._

  def routes = HttpRoutes.of[Task] {
    case GET -> Root / "media"  => Ok("world")
    case req @ POST -> Root / "media" => req.decode[MediaData](m => Ok(m))
  }
}
