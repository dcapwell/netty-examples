package com.github.dcapwell.netty.examples.block.v2

import com.github.dcapwell.netty.examples.Client
import com.google.common.base.Charsets
import com.google.common.primitives.{Ints, Longs}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelHandler, ChannelHandlerContext, ChannelInboundHandlerAdapter}

object BlockClient extends App {
  new Client {
    override def port: Int = 61340

    override def pipeline: List[ChannelHandler] = List(
      new ChannelInboundHandlerAdapter {
        override def channelActive(ctx: ChannelHandlerContext): Unit = {
          val blockId = BlockId(10)
          val data = "foo bar baz".getBytes(Charsets.UTF_8)

          // send request header
          val reqHeader = RequestHeader(CurrentVersion, RequestType.PutBlock)
          ctx.write(ctx.alloc().buffer(Longs.BYTES).writeLong(reqHeader.version.value))
          ctx.write(ctx.alloc().buffer(Ints.BYTES).writeInt(reqHeader.tpe.id))

          // send put header
          val put = PutBlock(blockId)
          //        ctx.writeAndFlush(ctx.alloc().buffer(Longs.BYTES).writeLong(reqHeader.blockId.value))
          ctx.write(ctx.alloc().buffer(Longs.BYTES).writeLong(put.blockId.value))


          // then send only packet
          val packetHeader = PacketHeader(0, 0, data.length, true)
          val packet = PutPacket(packetHeader, data)
          ctx.write(ctx.alloc().buffer(Longs.BYTES).writeLong(packet.header.blockOffset))
          ctx.write(ctx.alloc().buffer(Longs.BYTES).writeLong(packet.header.sequenceNum))
          ctx.write(ctx.alloc().buffer(Ints.BYTES).writeInt(packet.header.length))
          ctx.write(ctx.alloc().buffer(1).writeBoolean(packet.header.last))
          ctx.writeAndFlush(Unpooled.wrappedBuffer(packet.data))
        }

        override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = msg match {
          case in: ByteBuf if in.readableBytes() >= Ints.BYTES + Longs.BYTES =>
            val header = ResponseHeader(ResponseType(in.readInt()), BlockId(in.readLong()))
            println(s"Header: $header")
            ctx.close()
          case _ => println("=D;  not there yet!")
        }
      }
    )
  }
}
