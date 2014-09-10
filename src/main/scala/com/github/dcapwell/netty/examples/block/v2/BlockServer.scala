package com.github.dcapwell.netty.examples.block.v2

import java.util

import com.github.dcapwell.netty.examples.Server
import com.github.dcapwell.netty.examples.block.{BlockStore, ConcurrentBlockStore}
import com.google.common.primitives.{Ints, Longs}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelHandler, ChannelHandlerContext}
import io.netty.handler.codec.{ByteToMessageDecoder, MessageToMessageDecoder}

import scala.collection.mutable.ListBuffer

/**
 * Protocol is as follows
 * request header
 * request
 *
 * request can be one of two types:  Get, Put
 *
 * For put requests, the format is as follows
 *
 * (repeat till done)
 * packet header
 * data
 */
object BlockServer extends Server {
  lazy val store: BlockStore[BlockId] = {
    val store = new ConcurrentBlockStore[BlockId]

    store add(BlockId(0), "Hello World!".getBytes())
    store add(BlockId(1), "Its me, Tachyon!".getBytes())
    store
  }

  override def workerHandlers(): List[ChannelHandler] = List(
    new RequestHeaderDecoder(store)
  )
}

class RequestHeaderDecoder(store: BlockStore[BlockId]) extends ByteToMessageDecoder {
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes() >= Request.HeaderSize) {
      val header = RequestHeader(version = Version(in.readLong()),
        tpe = RequestType(in.readInt()))
      header.tpe match {
        case RequestType.GetBlock =>
          ctx.pipeline().addLast(getBlockPipeline: _*)

        case RequestType.PutBlock =>
          ctx.pipeline().addLast(putBlockPipeline: _*)
      }

      ctx.pipeline().remove(this)
    }
  }

  private[this] def getBlockPipeline: List[ChannelHandler] = List(
    new GetPacketDecoder(store)
  )

  private[this] def putBlockPipeline: List[ChannelHandler] = List(
    new PutDecoder(store)
  )
}

class GetPacketDecoder(store: BlockStore[BlockId]) extends ByteToMessageDecoder {
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes() >= Request.GetBlockSize) {
      val request = GetBlock(BlockId(in.readLong()), wrap(in.readInt()), wrap(in.readInt()))
      val blockId = request.blockId
      store(blockId) match {
        case Some(data) =>
          val header = ResponseHeader(ResponseType.GetBlockResponse)
          ctx.write(ctx.alloc().buffer(Ints.BYTES).writeInt(header.tpe.id))

          val rsp = GetBlockResponse(blockId, data.length, data)
          ctx.write(ctx.alloc().buffer(Longs.BYTES).writeLong(rsp.blockId.value))
          ctx.write(ctx.alloc().buffer(Ints.BYTES).writeInt(rsp.length))
          ctx.writeAndFlush(Unpooled.wrappedBuffer(rsp.data))
        case None =>
          val header = ResponseHeader(ResponseType.BlockNotFound)
          ctx.write(ctx.alloc().buffer(Ints.BYTES).writeInt(header.tpe.id))

          val rsp = BlockNotFound(blockId)
          ctx.writeAndFlush(ctx.alloc().buffer(Longs.BYTES).writeLong(rsp.blockId.value))
      }
    }
  }

  private[this] def wrap(value: Int): Option[Int] = if (value >= 0) Some(value) else None
}

class PutDecoder(store: BlockStore[BlockId]) extends ByteToMessageDecoder {
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes() >= Request.PutSize) {
      val header = PutBlock(BlockId(in.readLong()))
      //      ctx.pipeline().addLast(new PutPacketDecoder, new PacketAccumulator(header.blockId, store), new PutBlockSuccessResponder)
      ctx.pipeline().addLast("packet parser", new PutPacketDecoder)
      ctx.pipeline().addLast("packet accumulator", new PacketAccumulator(header.blockId, store))
      ctx.pipeline().addLast("success response", new PutBlockSuccessResponder)
      ctx.pipeline().remove(this)
    }
  }
}

class PutPacketDecoder extends ByteToMessageDecoder {
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes() >= Request.PacketHeaderSize) {
      val readerIndex = in.readerIndex()

      val header = PacketHeader(blockOffset = in.readLong(),
        sequenceNum = in.readLong(),
        length = in.readInt(),
        last = in.readBoolean())

      if (in.readableBytes() < header.length)
      // not enough data yet, so wait
        in.readerIndex(readerIndex)
      else {
        val data = Array.ofDim[Byte](header.length)
        in.readBytes(data)

        out.add(PutPacket(header, data))
        ctx.pipeline().remove(this)
        //TODO there may be more data in the buff, how do i make sure its processed?
      }
    }
  }
}

class PacketAccumulator(blockId: BlockId, store: BlockStore[BlockId]) extends MessageToMessageDecoder[PutPacket] {

  val data: ListBuffer[Array[Byte]] = ListBuffer()

  override def decode(ctx: ChannelHandlerContext, msg: PutPacket, out: util.List[AnyRef]): Unit = {
    if (msg.header.last) {
      data += msg.data
      store.add(blockId, merge())
      out.add(PutBlockSuccess(blockId))
      ctx.pipeline().remove(this)
    }
    else data += msg.data
  }

  private[this] def merge(): Array[Byte] = {
    val accumulated = Array.ofDim[Byte](data.map(_.length).sum)
    var index = 0
    data.foreach { bits =>
      System.arraycopy(bits, 0, accumulated, index, bits.length)
      index += bits.length
    }
    accumulated
  }
}

class PutBlockSuccessResponder extends MessageToMessageDecoder[PutBlockSuccess] {
  override def decode(ctx: ChannelHandlerContext, msg: PutBlockSuccess, out: util.List[AnyRef]): Unit = {
    val header = ResponseHeader(ResponseType.PutBlockSuccess)
    ctx.write(ctx.alloc().buffer(Ints.BYTES).writeInt(header.tpe.id))
    ctx.write(ctx.alloc().buffer(Longs.BYTES).writeLong(msg.blockId.value))
    ctx.flush()
  }
}
