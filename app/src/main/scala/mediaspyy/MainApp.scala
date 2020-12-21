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

import MediaStorage._
import AuthenticationService._
import zio.clock.Clock

object MainApp extends zio.App {

  type AppEnv = MediaStorage with AuthenticationService with Clock

  type AppTask[A] = RIO[AppEnv, A]

  val appEnv = MediaStorage.inMemory ++ AuthenticationService.testStub

  override def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] = {
    val ex = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4));
    val loggingEnv = Slf4jLogger.make((ctx, msg) => msg)

    (
      for {
        _ <- log.info("Statup")
        httpApp = Router[AppTask]("/" -> new ApiRoutes[AppEnv] {}.routes).orNotFound
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
      .provideCustomLayer(loggingEnv ++ appEnv)
      .exitCode

  }

}
