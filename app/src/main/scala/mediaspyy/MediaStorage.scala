package mediaspyy

import zio._

object MediaStorage {

  type MediaStorage = Has[MediaStorage.Service]

  trait Service {
    def createMedia(user: User, media: MediaData): IO[DbError, Unit]
    def list(user: User, resultSize: Int): IO[DbError, List[MediaData]]
  }

  val inMemory: Layer[Nothing, MediaStorage] = ZLayer.fromEffect(
    Ref
      .make(Map[User, List[MediaData]]())
      .map(ref =>
        new Service {
          override def createMedia(user: User, media: MediaData)
              : IO[DbError, Unit] =
            ref.update(m =>
              m.updatedWith(user)(o =>
                o match {
                  case None    => Some(List(media))
                  case Some(l) => Some(l :+ media)
                }
              )
            )
          override def list(user: User, resultSize: Int)
              : IO[DbError, List[MediaData]] =
            ref.get.map(m =>
              m.getOrElse(user, Nil)
                .reverse
                .take(resultSize)
            )
        }
      )
  )

  def createMedia(
      user: User,
      media: MediaData
  ): ZIO[MediaStorage, DbError, Unit] =
    ZIO.accessM(_.get.createMedia(user, media))

  def list(
      user: User,
      resultSize: Int
  ): ZIO[MediaStorage, DbError, List[MediaData]] =
    ZIO.accessM(_.get.list(user, resultSize))

  case class DbError(msg: String, cause: Throwable = null)
      extends RuntimeException(msg, cause)
}
