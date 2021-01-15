package mediaspyy

import zio._

import mediaspyy.MediaStorage._
import java.time.Instant
import zio.clock.Clock

object MediaService {

  type MediaService = Has[MediaService.Service]

  trait Service {
    def createMedia(
        user: User,
        media: MediaData
    ): IO[ProcessingError, MediaData]
    def list(user: User, resultSize: Int): IO[ProcessingError, List[MediaData]]
  }

  val storing: URLayer[MediaStorage, MediaService] =
    ZLayer.fromService[MediaStorage.Service, MediaService.Service](ms =>
      new Service {

        override def createMedia(
            user: User,
            media: MediaData
        ): IO[ProcessingError, MediaData] = ms
          .createMedia(user, media)
          .mapError(db => ProcessingError(s"Failed to store media $media", db))
          .map(_ => media)

        override def list(
            user: User,
            resultSize: Int
        ): IO[ProcessingError, List[MediaData]] = ms
          .list(user, resultSize)
          .mapError(db =>
            ProcessingError(
              s"Failed to load results for user $user and size $resultSize from storage $ms",
              db
            )
          )
      }
    )

  val withTimestamp: URLayer[MediaService with Clock, MediaService] =
    ZLayer.fromServices[MediaService.Service, Clock.Service, MediaService.Service]((s, c) =>
      new Service {

        override def createMedia(
            user: User,
            media: MediaData
        ): IO[ProcessingError, MediaData] = {

          val m = for {
            i <- c.instant
            mm = media.copy(createdAt = i)
            res <- s.createMedia(user, mm)
          } yield res

          m
        }

        override def list(
            user: User,
            resultSize: Int
        ): IO[ProcessingError, List[MediaData]] = s.list(user, resultSize)
      }
    )

  def createMedia(
      user: User,
      media: MediaData
  ): ZIO[MediaService, ProcessingError, MediaData] =
    ZIO.accessM(_.get.createMedia(user, media))

  def createMedia(
      user: User,
      bm: BasicMediaData
  ): ZIO[MediaService, ProcessingError, MediaData] = {

    val media = MediaData(
      title = bm.title,
      artist = bm.artist,
      album = bm.album,
      locations = bm.locations,
      images = bm.images,
      createdAt = Instant.EPOCH
    )
    ZIO.accessM(_.get.createMedia(user, media))
  }

  def list(
      user: User,
      resultSize: Int
  ): ZIO[MediaService, ProcessingError, List[MediaData]] =
    ZIO.accessM(_.get.list(user, resultSize))

  case class ProcessingError(msg: String, cause: Throwable = null)
      extends RuntimeException(msg, cause)

}
