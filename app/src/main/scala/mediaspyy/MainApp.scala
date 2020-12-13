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

object MainApp extends zio.App {

  override def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] = {
    val ex = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4));
    val loggingEnv = Slf4jLogger.make((ctx, msg) => msg)

    (
      for {
        _ <- log.info("Statup")
        httpApp = Router[Task]("/" -> new ApiRoutes {}.routes).orNotFound
        server <- ZIO.runtime[ZEnv].flatMap { implicit rts =>
          BlazeServerBuilder[Task](ex)
            .bindHttp(8080, "0.0.0.0")
            .withHttpApp(httpApp)
            .serve
            .compile
            .drain
        }
      } yield server
    )
      .provideCustomLayer(loggingEnv)
      .exitCode

  }

}
