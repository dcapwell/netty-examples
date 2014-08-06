package com.github.dcapwell.netty.examples

import java.util
import java.util.Date

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder

/**
 * Returns the current time when a connection is opened.
 *
 * To call this code, use the linux `rdate` command. If you call with telnet you will get funny output
 * like `׊9`.
 */
class TimeServerHandler extends ChannelInboundHandlerAdapter {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    val data = ctx.alloc().buffer(4) // writing a int, so 4 bytes
    data.writeInt((System.currentTimeMillis() / 1000l + 2208988800L).asInstanceOf[Int])

    val future = ctx.writeAndFlush(data)

    future.addListener((f: ChannelFuture) => ctx.close())
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}

object TimeServerHandler extends Server {
  override def workerHandlers(): List[ChannelHandler] = List(new TimeServerHandler())
}

class TimeDecoder extends ByteToMessageDecoder {
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: java.util.List[AnyRef]): Unit = {
    if(in.readableBytes() >= 4)
      out.add(in.readBytes(4))
  }
}

class TimeClientHandler extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    val m = msg.asInstanceOf[ByteBuf]
    try {
      val currentTimeMillis = (m.readUnsignedInt() - 2208988800L) * 1000L
      println(new Date(currentTimeMillis))
      ctx.close()
    } finally {
      m.release()
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}

object TimeClient extends Client {
  override def port: Int = 58040

  override def pipeline: List[ChannelHandler] = List(new TimeDecoder(), new TimeClientHandler())
}
