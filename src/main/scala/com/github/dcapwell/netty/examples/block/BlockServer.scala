package com.github.dcapwell.netty.examples.block

import java.util

import com.github.dcapwell.netty.examples.Server
import com.google.common.primitives.Longs
import io.netty.buffer.{Unpooled, ByteBuf}
import io.netty.channel._
import io.netty.handler.codec.{ByteToMessageDecoder, MessageToMessageDecoder}

object BlockServer extends Server {
  lazy val store: BlockStore = {
    val store = new ConcurrentBlockStore

    store add(BlockId(0), "Hello World!".getBytes())
    store add(BlockId(1), "Its me, Tachyon!".getBytes())
    store
  }

  override def workerHandlers(): List[ChannelHandler] = List(
    new HeaderDecoder,
    new MessageDecoder(store),
    new ResponseWriter
  )
}

import MessageType._

class HeaderDecoder extends ByteToMessageDecoder {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes() >= RequestHeader.Size) {
      val readerIndex = in.readerIndex()
      val header = parseHeader(in)

      println(s"Header: $header")

      // reset in for next iteration if not enough data
      if (in.readableBytes() < header.messageSize.value) in.readerIndex(readerIndex)
      else {
        out add parseMessage(in, header.messageType)
        ctx.pipeline().remove(this)
      }
    }
  }

  private[this] def parseHeader(buffer: ByteBuf): RequestHeader = RequestHeader(
    version = Version(buffer.readLong()),
    messageType = MessageType(buffer.readInt()),
    messageSize = Size(buffer.readInt()))

  private[this] def parseMessage(buf: ByteBuf, tpe: MessageType.MessageType): Message = tpe match {
    case Get =>
      GetBlock(BlockId(buf.readLong()), Message.wrap(buf.readInt()), Message.wrap(buf.readInt()))
  }
}

class MessageDecoder(store: BlockStore) extends MessageToMessageDecoder[Message] {
  override def decode(ctx: ChannelHandlerContext, msg: Message, out: util.List[AnyRef]): Unit = msg match {
    case GetBlock(blockId, offset, length) =>
      val result = for {
        data <- store(blockId).right
      } yield GetBlockResponse(blockId, data.slice(offset.getOrElse(0), length.getOrElse(data.size)))

      result.fold(out.add, out.add)
  }
}

class ResponseWriter extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    // netty will make sure this is true.
    val f: ChannelFuture = msg.asInstanceOf[MessageResponse] match {
      case GetBlockResponse(blockId, data) =>
        val size = data.size + Longs.BYTES
        // write long doesn't seem to have a handler...
        ctx.write(wrap(ctx, blockId.value))
        ctx.write(wrap(data))
      case BlockNotFound(blockId) =>
        ctx.write(wrap(ctx, blockId.value))
    }
    f.addListener(ChannelFutureListener.CLOSE)
  }

  private[this] def wrap(ctx: ChannelHandlerContext, value: Long): ByteBuf =
    ctx.alloc().buffer(Longs.BYTES).writeLong(value)

  private[this] def wrap(data: Array[Byte]): ByteBuf =
    Unpooled.wrappedBuffer(data)

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit =
    ctx.flush()
}



