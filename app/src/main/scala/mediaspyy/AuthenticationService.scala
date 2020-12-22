package mediaspyy

import zio._

object AuthenticationService {

  type AuthenticationService = Has[AuthenticationService.Service]

  trait Service {
    def authenticate(name: String, password: String): Task[Option[User]]
  }

  val testStub: Layer[Nothing, AuthenticationService] =
    ZLayer.succeed {
      new Service {
        val users = Map(
          ("test", "test"),
          ("foo", "bar")
        )
        override def authenticate(
            name: String,
            password: String
        ): Task[Option[User]] =
          Task {
            users
              .get(name)
              .filter(p => p == password)
              .map(_ => new User(name, password))
          }
      }
    }

  def authentcate(
      name: String,
      password: String
  ): RIO[AuthenticationService, Option[User]] =
    RIO.accessM(_.get.authenticate(name, password))

}
