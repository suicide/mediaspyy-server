package mediaspyy

import zio._
import zio.logging._
import zio.json._

import MediaService._
import AppConfig._
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTP
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

object FtpUploadMediaService {

  import DataJson._

  // handle concurrent upload requests
  // upload latest create media
  // TODO upload latest history

  val service: URLayer[AppConfig with Logging with MediaService, MediaService] =
    ZLayer.fromServicesM[
      AppConfig.Config,
      Logger[String],
      MediaService.Service,
      Any,
      Nothing,
      MediaService.Service
    ]((config, logger, mediaService) =>
      for {
        sem <- Semaphore.make(1)
      } yield new Service {

        override def createMedia(
            user: User,
            media: MediaData
        ): IO[ProcessingError, MediaData] = {
          def upload(media: MediaData) = sem.withPermit {
            ftpClient
              .use { client =>
                for {
                  // TODO mediaData to JSON
                  json <- toJsonString(media)
                  _ <- logger.debug(s"Created payload for media $media")
                  data <- Task(
                    new ByteArrayInputStream(
                      json.getBytes(StandardCharsets.UTF_8)
                    )
                  )
                  _ <- Task { client.storeFile(config.ftp.uploadPath, data) }
                    .filterOrDieMessage(identity)(
                      s"Uploading media $media unsuccessful"
                    )
                  _ <- logger.debug(s"Upload of media $media successful")
                } yield (())

              }
              .mapError(ex => ProcessingError("Failed to upload to server", ex))
          }

          for {
            res <- mediaService.createMedia(user, media)
            _ <- upload(res)
          } yield res
        }

        override def list(
            user: User,
            resultSize: Int
        ): IO[ProcessingError, List[MediaData]] =
          mediaService.list(user, resultSize)

        // TODO actually implement this
        def toJsonString(media: MediaData): Task[String] =
          Task(media.toJson)

        val ftpClient: TaskManaged[FTPClient] = {
          val c = config.ftp
          Managed
            .make(
              for {
                _ <- logger.debug(s"Connecting to ${c.server}:${c.port}")
                client <- Task {
                  val client = new FTPClient()
                  client.connect(c.server, c.port)
                  client.setFileType(FTP.BINARY_FILE_TYPE)
                  client
                }
              } yield client
            )(client =>
              Task { client.disconnect() }.catchAllCause(ex =>
                logger.error(s"Failed to disconnect from ftp server", ex)
              )
            )
            .flatMap(client =>
              Managed.make(Task {
                client.login(c.user, c.password)
              }.filterOrDieMessage(identity)("Unable to login").map(_ => client))(
                client =>
                  Task(client.logout()).catchAllCause(ex =>
                    logger.error(s"Failed to logout from server", ex)
                  )
              )
            )
        }
      }
    )
}
