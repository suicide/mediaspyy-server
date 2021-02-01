package mediaspyy

import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import org.http4s.client.blaze._
import org.http4s.client._

object Http4sClient {

  lazy val clientEx =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4));

  type ClientTask[A] = Task[A]

  type Http4sClient = Has[Client[ClientTask]]

  val client: TaskLayer[Http4sClient] = ZLayer.fromManaged{
    ZManaged.runtime[Any].flatMap { implicit rts =>

      BlazeClientBuilder[ClientTask](clientEx).resource.toManagedZIO
    }
  }

}
