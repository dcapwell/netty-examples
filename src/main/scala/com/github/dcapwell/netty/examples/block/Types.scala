package com.github.dcapwell.netty.examples.block

import com.google.common.primitives.{Ints, Longs}

case class BlockId(value: Long) extends AnyVal

case class Version(value: Long) extends AnyVal

case class Size(value: Int) extends AnyVal


case class Request(header: RequestHeader, message: Message)

case class RequestHeader(version: Version, messageType: MessageType.MessageType, messageSize: Size)

object RequestHeader {
  // version + type, message size
  val Size: Int = Longs.BYTES + Ints.BYTES + Ints.BYTES
}

case class Response(header: ResponseHeader, response: MessageResponse)

object Response {
  def apply(msg: MessageResponse): Response = msg match {
    case GetBlockResponse(_, data) =>
      val size = Size(data.size + Longs.BYTES)
      val header = ResponseHeader(MessageResponseType.GetBlockResponse, size)
      Response(header, msg)
    case BlockNotFound(blockId) =>
      val header = ResponseHeader(MessageResponseType.BlockNotFound)
      Response(header, msg)
  }
}

case class ResponseHeader(version: Version,
                          messageType: MessageResponseType.MessageResponseType,
                          messageSize: Size)

object ResponseHeader {
  // version + type, message size
  val Size: Int = Longs.BYTES + Ints.BYTES + Ints.BYTES

  def apply(messageType: MessageResponseType.MessageResponseType, messageSize: Size): ResponseHeader =
    ResponseHeader(CurrentVersion, messageType, messageSize)

  def apply(messageType: MessageResponseType.MessageResponseType): ResponseHeader =
    ResponseHeader(CurrentVersion, messageType, Empty)
}
