package mediaspyy

import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import MediaService._
import Http4sClient._
import AppConfig._

import org.http4s._
import org.http4s.client._
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.Method._
import cats.effect.ConcurrentEffect

object BotPushMediaService {

  // http4s client
  // auth data for bot api + coordinates
  // prepage command to place on bot api

  // TODO map user to bot instance
  //
  val botPush
      : RLayer[AppConfig with Http4sClient with MediaService, MediaService] =
    ZLayer.fromServicesM[AppConfig.Config, Client[
      ClientTask
    ], MediaService.Service, Any, Throwable, MediaService.Service](
      (config, client, mediaService) =>
        ZIO.runtime[Any].map { implicit rts =>
          new MediaService.Service {
            override def createMedia(
                user: User,
                media: MediaData
            ): IO[ProcessingError, MediaData] = {

              (for {
                data <- mediaService.createMedia(user, media)
                _ <- update(data)
              } yield data)
            }

            def update(
                media: MediaData
            ): IO[ProcessingError, MediaData] = {
              val c = config.bot

              (for {
                req <- createRequest(c, media)
                res <- client.status(req)
                if res == Status.Ok
              } yield media)
                .mapError(t =>
                  ProcessingError("Failed to send media info to bot", t)
                )
            }

            override def list(
                user: User,
                resultSize: Int
            ): IO[ProcessingError, List[MediaData]] =
              mediaService.list(user, resultSize)

            def createRequest(config: BotConfig, media: MediaData)
                : Task[Request[ClientTask]] = {
              for {
                baseUri <- Task.fromEither(Uri.fromString(config.addMediaUri))
                commandUri = baseUri / config.keyword
                song = s"${media.title} by ${media.artist}"
                body = EntityEncoder
                  .stringEncoder[ClientTask]
                  .toEntity(song)
                  .body
              } yield Request(
                method = PUT,
                uri = commandUri,
                headers = Headers.of(
                  Authorization(BasicCredentials(config.user, config.password))
                ),
                body = body
              )

            }

            override def delete(user: User, id: String)
                : IO[ProcessingError, Unit] = {
                  for {
                    delete <- mediaService.delete(user, id)
                    media <- mediaService.list(user, 1)
                    _ <- media match {
                      case head :: next => update(head)
                      case Nil => ZIO.unit
                    }
                  } yield delete
                }
          }
        }
    )
}
