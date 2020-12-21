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
        val test = "test"
        override def authenticate(
            name: String,
            password: String
        ): Task[Option[User]] =
          if (name == test && password == test) {
            return Task(Some(new User(test, test)))
          } else {
            return Task(None)
          }
      }
    }

  def authentcate(
      name: String,
      password: String
  ): RIO[AuthenticationService, Option[User]] =
    RIO.accessM(_.get.authenticate(name, password))

}
