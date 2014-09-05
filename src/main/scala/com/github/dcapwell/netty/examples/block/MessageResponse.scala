package com.github.dcapwell.netty.examples.block

import io.netty.buffer.{Unpooled, ByteBuf}

trait MessageResponse extends Any with Writable

case class GetBlockResponse(blockId: BlockId, data: Array[Byte]) extends MessageResponse {
  override def write(out: ByteBuf): Unit = {
    blockId write out
    out.writeBytes(Unpooled.wrappedBuffer(data))
  }
}

case class PutBlockSuccess(blockId: BlockId) extends MessageResponse {
  override def write(out: ByteBuf): Unit = blockId.write(out)
}

case class BlockNotFound(blockId: BlockId) extends MessageResponse {
  override def write(out: ByteBuf): Unit = blockId.write(out)
}

object MessageResponseType extends Enumeration {

  type MessageResponseType = Value
  val GetBlockResponse, PutBlockSuccess, BlockNotFound = Value

  def write(responseType: MessageResponseType, buf: ByteBuf) = {
    buf.writeInt(responseType.id)
  }
}
