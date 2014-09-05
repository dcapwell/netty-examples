package com.github.dcapwell.netty.examples.block

import java.util

import com.github.dcapwell.netty.examples.Client
import com.google.common.base.Charsets
import io.netty.buffer.{Unpooled, ByteBuf}
import io.netty.channel._
import io.netty.handler.codec.{MessageToByteEncoder, MessageToMessageEncoder}

object BlockClient extends Client {
  override def port: Int = 56988

  override def pipeline: List[ChannelHandler] = List(
    new HeaderEncoder,
    new MessageEncoder,
    new RequestEncoder,
    new PutWorker
  )
}

abstract class PrintReader extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    val in = msg.asInstanceOf[ByteBuf]
    if (in.readableBytes() > 4) {
      println(s"Version: ${in.readLong()}")
      println(s"Type: ${MessageResponseType(in.readInt())}")
      println(s"Size: ${in.readInt()}")
      println(in.toString(Charsets.UTF_8))
      ctx.close()
    }
  }
}

class GetWorker extends PrintReader {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.writeAndFlush(get(BlockId(10)))
  }

  private[this] def get(blockId: BlockId): Request = {
    val msg = GetBlock(blockId, None, None)
    val header = RequestHeader(CurrentVersion, MessageType.Get, Size(Message.GetBlockSize))
    Request(header, msg)
  }
}

class PutWorker extends PrintReader {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.writeAndFlush(put(BlockId(10), "This is data that I would like to save".getBytes()))
  }

  private[this] def put(blockId: BlockId, data: Array[Byte]): Request = {
    val msg = PutBlock(blockId, data)
    val header = RequestHeader(CurrentVersion, MessageType.Put, Size(Message.putSize(data)))
    Request(header, msg)
  }
}

class MessageEncoder extends MessageToByteEncoder[Message] {
  override def encode(ctx: ChannelHandlerContext, msg: Message, out: ByteBuf): Unit = msg match {
    case GetBlock(blockId, offset, length) =>
      blockId.write(out)
      out.writeInt(Message.unwrap(offset))
      out.writeInt(Message.unwrap(length))

    case PutBlock(blockId, data) =>
      blockId.write(out)
      out.writeBytes(Unpooled.wrappedBuffer(data))
  }
}

class HeaderEncoder extends MessageToByteEncoder[RequestHeader] {
  override def encode(ctx: ChannelHandlerContext, msg: RequestHeader, out: ByteBuf): Unit = {
    out.writeLong(msg.version.value)
    out.writeInt(msg.messageType.id)
    out.writeInt(msg.messageSize.value)
  }
}

class RequestEncoder extends MessageToMessageEncoder[Request] {
  override def encode(ctx: ChannelHandlerContext, msg: Request, out: util.List[AnyRef]): Unit = {
    out.add(msg.header)
    out.add(msg.message)
  }
}