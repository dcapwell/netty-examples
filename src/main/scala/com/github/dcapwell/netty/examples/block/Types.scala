package com.github.dcapwell.netty.examples.block

import com.google.common.primitives.{Ints, Longs}
import io.netty.buffer.ByteBuf

case class BlockId(value: Long) extends AnyVal with Writable {
  override def write(out: ByteBuf): Unit = out.writeLong(value)
}

case class Version(value: Long) extends AnyVal with Writable {
  override def write(out: ByteBuf): Unit = out.writeLong(value)
}

case class Size(value: Int) extends AnyVal with Writable {
  override def write(out: ByteBuf): Unit = out.writeInt(value)
}


case class Request(header: RequestHeader, message: Message)

case class RequestHeader(version: Version, messageType: MessageType.MessageType, messageSize: Size)

object RequestHeader {
  // version + type, message size
  val Size: Int = Longs.BYTES + Ints.BYTES + Ints.BYTES
}

case class Response(header: ResponseHeader, response: MessageResponse) extends Writable {
  override def write(out: ByteBuf): Unit = {
    header.write(out)
    response.write(out)
  }
}

object Response {
  def apply(msg: MessageResponse): Response = msg match {
    case GetBlockResponse(_, data) =>
      val size = Size(data.size + Longs.BYTES)
      val header = ResponseHeader(MessageResponseType.GetBlockResponse, size)
      Response(header, msg)
    case PutBlockSuccess(_) =>
      val size = Size(Longs.BYTES)
      val header = ResponseHeader(MessageResponseType.PutBlockSuccess, size)
      Response(header, msg)
    case BlockNotFound(blockId) =>
      val header = ResponseHeader(MessageResponseType.BlockNotFound)
      Response(header, msg)
  }
}

case class ResponseHeader(version: Version,
                          messageType: MessageResponseType.MessageResponseType,
                          messageSize: Size) extends Writable {
  override def write(out: ByteBuf): Unit = {
    version.write(out)
    MessageResponseType.write(messageType, out)
    messageSize.write(out)
  }
}

object ResponseHeader {
  // version + type, message size
  val Size: Int = Longs.BYTES + Ints.BYTES + Ints.BYTES

  def apply(messageType: MessageResponseType.MessageResponseType, messageSize: Size): ResponseHeader =
    ResponseHeader(CurrentVersion, messageType, messageSize)

  def apply(messageType: MessageResponseType.MessageResponseType): ResponseHeader =
    ResponseHeader(CurrentVersion, messageType, Empty)
}
