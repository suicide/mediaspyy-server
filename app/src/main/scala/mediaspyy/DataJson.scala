package mediaspyy

import zio.json._

object DataJson {

  implicit val mediaDataEncoder: JsonEncoder[MediaData] = {
    implicit val location = DeriveJsonEncoder.gen[MediaLocation]
    implicit val image = DeriveJsonEncoder.gen[MediaImage]
    DeriveJsonEncoder.gen[MediaData]
  }

  implicit val basicMediaDataDecoder: JsonDecoder[BasicMediaData] = {

    implicit val location = DeriveJsonDecoder.gen[MediaLocation]
    implicit val image = DeriveJsonDecoder.gen[MediaImage]
    DeriveJsonDecoder.gen[BasicMediaData]
  }

}
