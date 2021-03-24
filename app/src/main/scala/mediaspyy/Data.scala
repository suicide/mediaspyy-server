package mediaspyy

import java.time.Instant

case class BasicMediaData(
  title: String,
  artist: String,
  album: Option[String],
  locations: List[MediaLocation],
  images: List[MediaImage],
  )

case class MediaData(
  // TODO might change this?
  id: Option[String] = None,

  title: String,
  artist: String,
  album: Option[String],
  locations: List[MediaLocation],
  images: List[MediaImage],

  createdAt: Instant
  )

case class MediaLocation(
  `type`: String,
  url: String
  )

case class MediaImage (
  src: String,
  size: Option[String],
  `type`: Option[String]
  )

case class User (
  name: String,
  password: String
  )
