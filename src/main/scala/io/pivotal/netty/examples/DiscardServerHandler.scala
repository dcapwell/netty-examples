package io.pivotal.netty.examples

import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.util.{CharsetUtil, ReferenceCountUtil}

class DiscardServerHandler extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    val in = msg.asInstanceOf[ByteBuf];
    try {
      while (in.isReadable()) {
        // (1)
        val count = in.readableBytes()
        val bytes = Array.ofDim[Byte](count)
        in.readBytes(bytes)
        val str = new String(bytes, CharsetUtil.UTF_8)
        println(str)
        //        System.out.println(in.toString(io.netty.util.CharsetUtil.UTF_8))
        // toString doesn't consume the bytes, so would loop forever
      }
    } finally ReferenceCountUtil.release(msg)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}

object DiscardServerHandler extends Server {
  override def handler(): ChannelHandler = new DiscardServerHandler
}