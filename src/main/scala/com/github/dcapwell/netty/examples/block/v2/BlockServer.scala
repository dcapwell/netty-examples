package com.github.dcapwell.netty.examples.block.v2

import java.util

import com.github.dcapwell.netty.examples.Server
import com.github.dcapwell.netty.examples.block.{BlockStore, ConcurrentBlockStore}
import com.google.common.primitives.{Ints, Longs}
import io.netty.buffer.ByteBuf
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
        tpe = RequestType(in.readInt()),
        blockId = BlockId(in.readLong()))
      header.tpe match {
        case RequestType.GetBlock =>
          ctx.pipeline().addLast(getBlockPipeline(header.blockId): _*)

        case RequestType.PutBlock =>
          ctx.pipeline().addLast(putBlockPipeline(header.blockId): _*)
      }

      ctx.pipeline().remove(this)

      // in case there is more data, send it to out
      //      ctx.fireChannelRead(in.retain())
    }
  }

  private[this] def getBlockPipeline(blockId: BlockId): List[ChannelHandler] = List(

  )

  private[this] def putBlockPipeline(blockId: BlockId): List[ChannelHandler] = List(
    new PacketDecoder,
    new PacketAccumulator(blockId, store),
    new PutBlockSuccessResponder
  )
}

class PacketDecoder extends ByteToMessageDecoder {
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
      }
    }
  }
}

class PacketAccumulator(blockId: BlockId, store: BlockStore[BlockId]) extends MessageToMessageDecoder[PutPacket] {

  val data: ListBuffer[Array[Byte]] = ListBuffer()

  override def decode(ctx: ChannelHandlerContext, msg: PutPacket, out: util.List[AnyRef]): Unit = {
    if (msg.header.last) {
      store.add(blockId, merge())
      out.add(PutBlockSuccess(blockId))
      ctx.pipeline().remove(this)
    }
    else data +: msg.data
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
    val rsp = msg

    val header = ResponseHeader(ResponseType.PutBlockSuccess, rsp.blockId)

    ctx.write(ctx.alloc().buffer(Ints.BYTES).writeInt(header.tpe.id))
    ctx.write(ctx.alloc().buffer(Longs.BYTES).writeLong(rsp.blockId.value))
    ctx.flush()
  }
}
