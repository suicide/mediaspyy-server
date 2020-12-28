package mediaspyy

import zio._
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.MongoClient

import AppConfig.AppConfig

object MongoDb {

  type MongoDb = Has[MongoDatabase]

  val database: RLayer[AppConfig, MongoDb] = ZLayer.fromServiceManaged(config =>
    Managed
      .make(RIO(MongoClient(config.connectionString)))(client =>
        UIO(client.close())
      )
      .map(client => client.getDatabase(config.dbName))
  )

}
