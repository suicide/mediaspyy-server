package mediaspyy

import zio._

object AppConfig {

  type AppConfig = Has[DbConfig]

  case class DbConfig(connectionString: String, dbName: String)

  val hardDefault: ULayer[AppConfig] =
    ZLayer.succeed(
      DbConfig(
        "mongodb://root:root@localhost:27017/?authSource=admin&connectTimeoutMS=1000&socketTimeoutMS=1000&serverSelectionTimeoutMS=2000",
        "mediaspyy"
      )
    )
}
