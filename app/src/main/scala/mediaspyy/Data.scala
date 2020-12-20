package mediaspyy

case class MediaData(
  title: String,
  artist: String,
  album: Option[String],
  url: String,
  images: List[MediaImage]
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
