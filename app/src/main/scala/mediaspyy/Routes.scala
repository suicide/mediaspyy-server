package mediaspyy

import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Header

import zio.interop.catz._
import zio._
import cats.data.Kleisli
import zio.logging._
import zio.json._

import org.http4s.EntityDecoder
import org.http4s.EntityEncoder

import MediaService._
import AuthenticationService._
import DataJson._

import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.AuthedRoutes
import cats.data.OptionT
import cats.data.EitherT
import org.http4s.BasicCredentials
import org.http4s.server.middleware.authentication.BasicAuth
import java.nio.charset.StandardCharsets
import cats.effect.Sync
import org.http4s.MalformedMessageBodyFailure

trait ApiRoutes[R <: MediaService with AuthenticationService with Logging] {

  type ApiTask[A] = RIO[R, A]
  type Err = String

  implicit def jsonDecoder: EntityDecoder[ApiTask, BasicMediaData] = {

    val decoder: EntityDecoder[ApiTask, Array[Byte]] =
      EntityDecoder.byteArrayDecoder

    decoder.flatMapR(arr => {
      val res = basicMediaDataDecoder
        .decodeJson(new String(arr, StandardCharsets.UTF_8))

      res match {
        case Right(media) => EitherT.pure(media)
        case Left(msg)    => EitherT.leftT(MalformedMessageBodyFailure(msg))
      }
    })
  }

  implicit def jsonEncoder: EntityEncoder[ApiTask, List[MediaData]] =
    EntityEncoder.simple(Header("Content-Type", "application/json"))(m => {
      fs2.Chunk.array {
        m.toJsonPretty.getBytes(StandardCharsets.UTF_8)
      }
    })

  lazy val dsl: Http4sDsl[ApiTask] = Http4sDsl[ApiTask]
  import dsl._

  object resultSizeParam
      extends OptionalQueryParamDecoderMatcher[Int]("resultSize")

  val authedMiddleware: AuthMiddleware[ApiTask, User] =
    BasicAuth("mediaspyy", b => authentcate(b.username, b.password))

  def routes: HttpRoutes[ApiTask] = authedMiddleware(authedRoutes)

  def authedRoutes = AuthedRoutes.of[User, ApiTask] {
    case GET -> Root / "media" :? resultSizeParam(size) as user =>
      list(user, size.getOrElse(10))
        .foldM(
          e =>
            log.throwable("Request failed with error", e) *>
              InternalServerError("sorry"),
          Ok(_)
        )
    case req @ POST -> Root / "media" as user =>
      req.req.decode[BasicMediaData](m =>
        createMedia(user, m)
          .foldM(
            e =>
              log.throwable("Request failed with error", e) *>
                InternalServerError("sorry"),
            _ => Ok()
          )
      )
  }

}
