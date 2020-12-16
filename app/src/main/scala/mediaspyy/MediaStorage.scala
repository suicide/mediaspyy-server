package mediaspyy

import zio._

object MediaStorage {

  type MediaStorage = Has[MediaStorage.Service]

  trait Service {
    def createMedia(media: MediaData): IO[DbError, Unit]
    def list(resultSize: Int): IO[DbError, List[MediaData]]
  }

  val inMemory: Layer[Nothing, MediaStorage] = ZLayer.fromEffect(
    Ref
      .make(List[MediaData]())
      .map(ref =>
        new Service {
          override def createMedia(media: MediaData): IO[DbError, Unit] =
            ref.update(l => l :+ media)
          override def list(resultSize: Int): IO[DbError, List[MediaData]] =
            ref.get
        }
      )
  )

  def createMedia(media: MediaData): ZIO[MediaStorage, DbError, Unit] =
    ZIO.accessM(_.get.createMedia(media))

  def list(resultSize: Int): ZIO[MediaStorage, DbError, List[MediaData]] =
    ZIO.accessM(_.get.list(resultSize))

  case class DbError(msg: String, cause: Throwable)
      extends RuntimeException(msg, cause)
}
