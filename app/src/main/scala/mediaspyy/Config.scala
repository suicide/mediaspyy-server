package mediaspyy

import zio._
import zio.config.magnolia.describe
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config._

object AppConfig {

  type AppConfig = Has[Config]
  private val automaticConfig = descriptor[Config]

  case class Config(
      server: ServerConfig,
      db: DbConfig,
      users: Map[String, String],
      bot: BotConfig
  )

  case class ServerConfig(port: Int = 8080)
  case class DbConfig(connectionString: String, dbName: String)
  case class BotConfig(
      enabled: Boolean = false,
      addMediaUri: String =  "http://localhost:48080/command",
      user: String = "test",
      password: String = "test",
      keyword: String = "song"
  )

  val hardDefault =
    ZConfig.fromMap(
      Map(
        //"server.port" -> "8080",
        "db.connectionString" -> "mongodb://root:root@localhost:27017/?authSource=admin&connectTimeoutMS=1000&socketTimeoutMS=1000&serverSelectionTimeoutMS=2000",
        "db.dbName" -> "mediaspyy",
        "users.test" -> "test",
        "users.foo" -> "bar",
        "bot.enabled" -> "true",
      ),
      automaticConfig,
      keyDelimiter = Some('.')
    )

  val fromEnv = ZConfig.fromSystemEnv(
    configDescriptor = automaticConfig,
    keyDelimiter = Some('_')
  )
}
