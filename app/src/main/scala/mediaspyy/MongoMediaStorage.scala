package mediaspyy

import zio._
import zio.interop.reactivestreams._
import zio.stream._
import zio.logging._
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{
  fromRegistries,
  fromProviders
}
import mediaspyy.MediaStorage.DbError

object MongoMediaStorage {

  val storage
      : URLayer[MongoDb.MongoDb with Logging, MediaStorage.MediaStorage] =
    ZLayer.fromServices[MongoDatabase, Logger[String], MediaStorage.Service](
      (db, logging) => new MongoMediaStorage(db, logging)
    )

  val codecRegistry = fromRegistries(
    fromProviders(
      classOf[MediaEntry],
      classOf[MediaData],
      classOf[MediaImage],
      classOf[MediaLocation]
    ),
    DEFAULT_CODEC_REGISTRY
  )

  class MongoMediaStorage(
      private val db: MongoDatabase,
      private val logger: Logger[String]
  ) extends MediaStorage.Service {

    def collection: Task[MongoCollection[MediaEntry]] =
      Task(
        db.withCodecRegistry(codecRegistry)
          .getCollection[MediaEntry]("mediaEntry")
      )

    override def createMedia(
        user: User,
        media: MediaData
    ): IO[MediaStorage.DbError, Unit] = {

      val result = for {
        c <- collection.mapError(t => new DbError("", t))
        _ <- logger.debug(s"Inserting new media entry into collection $c")
        o <- c
          .insertOne(MediaEntry(user.name, media))
          .toStream()
          .run(Sink.head)
          .mapError(t => DbError("Failed to handle mongodb request", t))
        res <-
          o match {
            case None =>
              ZIO.fail(DbError("Did not receive result from mongodb"))
            case Some(r) =>
              if (r.wasAcknowledged)
                logger.debug(s"successfully added new entry $media") *> ZIO.unit
              else ZIO.fail(DbError("Create was not acknowleged"))
          }
      } yield res

      result
    }

    override def list(
        user: User,
        resultSize: Int
    ): IO[MediaStorage.DbError, List[MediaData]] = {

      val result = for {
        c <- collection.mapError(t => new DbError("", t))
        _ <- logger.debug(s"Reading media data for user $user from $c")
        chunk <- c
          .find(equal("username", user.name))
          .sort(descending("createdAt"))
          .toStream()
          .take(resultSize)
          .run(Sink.collectAll)
          .mapError(t => DbError("Failed to handle mongodb request", t))
        res = chunk.toList.map(e => e.media)
        _ <- logger.debug(s"Returning result list $res")
      } yield res

      result
    }

  }

  case class MediaEntry(username: String, media: MediaData)
}
