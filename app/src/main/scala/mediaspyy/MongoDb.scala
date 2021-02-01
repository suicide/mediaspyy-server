package mediaspyy

import zio._
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.MongoClient

import AppConfig.AppConfig

object MongoDb {

  type MongoDb = Has[MongoDatabase]

  val database: RLayer[AppConfig, MongoDb] = ZLayer.fromServiceManaged(config => {
      val c = config.db
    Managed
      .make(RIO(MongoClient(c.connectionString)))(client =>
        UIO(client.close())
      )
      .map(client => client.getDatabase(c.dbName))
  }
  )

}
