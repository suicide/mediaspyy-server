package mediaspyy

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

import zio.interop.catz._
import zio._

import io.circe.generic.auto._
import io.circe.Decoder
import io.circe.Encoder
import org.http4s.circe._
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder

import MediaStorage._

trait ApiRoutes[R <: MediaStorage] {

  type ApiTask[A] = RIO[R, A]

  implicit def circeJsonDecoder[A](implicit
      decoder: Decoder[A]
  ): EntityDecoder[ApiTask, A] = jsonOf[ApiTask, A]
  implicit def circeJsonEncoder[A](implicit
      decoder: Encoder[A]
  ): EntityEncoder[ApiTask, A] = jsonEncoderOf[ApiTask, A]

  lazy val dsl: Http4sDsl[ApiTask] = Http4sDsl[ApiTask]
  import dsl._

  def routes = HttpRoutes.of[ApiTask] {
    case GET -> Root / "media" =>
      list(10).foldM(_ => BadRequest(), Ok(_))
    case req @ POST -> Root / "media" =>
      req.decode[MediaData](m =>
        createMedia(m).foldM(_ => BadRequest(), _ => Ok())
      )
  }
}
