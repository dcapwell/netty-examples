package com.github.dcapwell.netty.examples.block.v1

import java.util

import com.github.dcapwell.netty.examples.Server
import com.github.dcapwell.netty.examples.block.{ConcurrentBlockStore, BlockStore}
import com.google.common.primitives.Longs
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.handler.codec.{ByteToMessageDecoder, MessageToByteEncoder}

object BlockServer extends Server {
  lazy val store: BlockStore[BlockId] = {
    val store = new ConcurrentBlockStore[BlockId]

    store add(BlockId(0), "Hello World!".getBytes())
    store add(BlockId(1), "Its me, Tachyon!".getBytes())
    store
  }

  override def workerHandlers(): List[ChannelHandler] = List(
    new RequestDecoder,
    new WritableEncoder,
    new ServerHandler(store)
  )
}

class RequestDecoder extends ByteToMessageDecoder {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes() >= RequestHeader.Size) {
      val readerIndex = in.readerIndex()
      val header = parseHeader(in)

      // reset in for next iteration if not enough data
      if (in.readableBytes() < header.messageSize.value) in.readerIndex(readerIndex)
      else {
        out add Request(header, parseMessage(in, header))
        ctx.pipeline().remove(this)
      }
    }
  }

  private[this] def parseHeader(buffer: ByteBuf): RequestHeader = RequestHeader(
    version = Version(buffer.readLong()),
    messageType = MessageType(buffer.readInt()),
    messageSize = Size(buffer.readInt()))

  private[this] def parseMessage(buf: ByteBuf, header: RequestHeader): Message = header.messageType match {
    case MessageType.Get =>
      GetBlock(BlockId(buf.readLong()), Message.wrap(buf.readInt()), Message.wrap(buf.readInt()))
    case MessageType.Put =>
      val blockId = BlockId(buf.readLong())
      val data = Array.ofDim[Byte](header.messageSize.value - Longs.BYTES)
      buf.readBytes(data, 0, data.length)
      PutBlock(blockId, data)
  }
}

class ServerHandler(store: BlockStore[BlockId]) extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    val rsp: Response = handle(msg.asInstanceOf[Request])
    ctx.write(rsp)
  }

  def handle(request: Request): Response = request.message match {
    case GetBlock(blockId, offset, length) =>
      store(blockId) match {
        case None => Response(BlockNotFound(blockId))
        case Some(data) =>
          Response(
            GetBlockResponse(blockId, data.slice(offset.getOrElse(0), length.getOrElse(data.size))))
      }
    case PutBlock(blockId, data) =>
      store.add(blockId, data)
      Response(PutBlockSuccess(blockId))
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = ctx.flush()
}

class WritableEncoder extends MessageToByteEncoder[Writable] {
  override def encode(ctx: ChannelHandlerContext, msg: Writable, out: ByteBuf): Unit = msg.write(out)
}