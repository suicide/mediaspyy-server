package mediaspyy

import zio._

import AppConfig._

object AuthenticationService {

  type AuthenticationService = Has[AuthenticationService.Service]

  trait Service {
    def authenticate(name: String, password: String): Task[Option[User]]
  }

  val fromConfig: URLayer[AppConfig, AuthenticationService] =
    ZLayer.fromService[Config, AuthenticationService.Service] { c =>
      new Service {
        override def authenticate(
            name: String,
            password: String
        ): Task[Option[User]] =
          Task {
            c.users
              .get(name)
              .filter(p => p == password)
              .map(_ => User(name, password))
          }
      }
    }

  def authentcate(
      name: String,
      password: String
  ): RIO[AuthenticationService, Option[User]] =
    RIO.accessM(_.get.authenticate(name, password))

}
