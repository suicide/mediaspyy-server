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
  // handle multiple users

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
          def upload[A](content: A, path: String)(implicit je: JsonEncoder[A]) = sem.withPermit {
            ftpClient
              .use { client =>
                for {
                  json <- Task(content.toJson)
                  _ <- logger.debug(s"Created payload for content $content")
                  data <- Task(
                    new ByteArrayInputStream(
                      json.getBytes(StandardCharsets.UTF_8)
                    )
                  )
                  _ <- Task { client.storeFile(path, data) }
                    .filterOrDieMessage(identity)(
                      s"Uploading content $content unsuccessful"
                    )
                  _ <- logger.debug(s"Upload of content $content successful")
                } yield (())

              }
              .mapError(ex => ProcessingError("Failed to upload to server", ex))
          }

          val c = config.ftp
          for {
            res <- mediaService.createMedia(user, media)
            _ <- upload(res, c.uploadPathCurrent)
            history <- list(user, 100)
            _ <- upload(history, c.uploadPathHistory)
          } yield res
        }

        override def list(
            user: User,
            resultSize: Int
        ): IO[ProcessingError, List[MediaData]] =
          mediaService.list(user, resultSize)

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
