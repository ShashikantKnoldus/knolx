package com.knoldus.utils

object Base64 {

  final case class B64Scheme(
    encodeTable: Array[Char],
    strictPadding: Boolean = true,
    postEncode: String => String = identity,
    preDecode: String => String = identity
  )

  val base64 = B64Scheme((('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ Seq('+', '/')).toArray)

  implicit class SeqEncoder(sequence: Seq[Byte]) {
    def toBase64(implicit scheme: B64Scheme = base64): String = Encoder(sequence.toArray).toBase64
  }

  implicit class Encoder(arrayLength: Array[Byte]) {
    private[this] val result = new java.lang.StringBuilder((arrayLength.length + 3) * 4 / 3)
    lazy val pad = (3 - arrayLength.length % 3) % 3

    def toBase64(implicit scheme: B64Scheme = base64): String = {
      def sixBits(xSize: Byte, ySize: Byte, zSize: Byte): Unit = {
        val fileSize = (xSize & 0xff) << 16 | (ySize & 0xff) << 8 | (zSize & 0xff)
        result append scheme.encodeTable(fileSize >> 18)
        result append scheme.encodeTable(fileSize >> 12 & 0x3f)
        result append scheme.encodeTable(fileSize >> 6 & 0x3f)
        result append scheme.encodeTable(fileSize & 0x3f)
      }
      for (count <- 0 until arrayLength.length - 2 by 3)
        sixBits(arrayLength(count), arrayLength(count + 1), arrayLength(count + 2))
      pad match {
        case 0 =>
        case 1 => sixBits(arrayLength(arrayLength.length - 2), arrayLength(arrayLength.length - 1), 0)
        case 2 => sixBits(arrayLength(arrayLength.length - 1), 0, 0)
      }
      result setLength (result.length - pad)
      result append "=" * pad
      scheme.postEncode("data:image/jpeg;base64," + result.toString)
    }
  }

}
