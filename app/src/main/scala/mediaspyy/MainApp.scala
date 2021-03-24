package mediaspyy

import zio._
import zio.logging._
import zio.logging.slf4j._
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s.server.Router
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

import AppConfig._
import Http4sClient._
import MediaStorage._
import MediaService._
import AuthenticationService._
import BotPushMediaService._
import FtpUploadMediaService._
import zio.clock.Clock
import zio.random.Random

object MainApp extends zio.App {

  type AppEnv = MediaService
    with MediaStorage
    with AuthenticationService
    with Clock
    with Logging

  type AppTask[A] = RIO[AppEnv, A]

  val appEnv = {
    import zio.ZLayer._

    val loggingEnv = Slf4jLogger.make((ctx, msg) => msg)

    val baseEnv =
      requires[Clock] ++ requires[Random] ++ loggingEnv ++ AppConfig.fromEnv
    val storageEnv = requires[Logging] ++ MongoDb.database >>>
      MongoMediaStorage.storage

    val serviceEnv = requires[Logging] ++ requires[Clock] ++ requires[Random] ++
      requires[AppConfig] ++ MediaService.storing >+>
      MediaService.withIdAndTimestamp >+>
      FtpUploadMediaService.service
//    Http4sClient.client >+> BotPushMediaService.botPush

    baseEnv >+>
      storageEnv >+>
      serviceEnv >+>
      AuthenticationService.fromConfig
  }

  override def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] = {
    val ex = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4));

    (
      for {
        _ <- log.info("Statup")
        httpApp = Router[AppTask](
          "/" -> new ApiRoutes[AppEnv] {}.routes
        ).orNotFound
        server <- ZIO.runtime[AppEnv].flatMap { implicit rts =>
          BlazeServerBuilder[AppTask](ex)
            .bindHttp(8080, "0.0.0.0")
            .withHttpApp(httpApp)
            .serve
            .compile
            .drain
        }
      } yield server
    )
      .provideCustomLayer(appEnv)
      .exitCode

  }

}
