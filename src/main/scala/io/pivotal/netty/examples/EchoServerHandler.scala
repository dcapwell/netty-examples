package io.pivotal.netty.examples

import io.netty.channel._

class EchoServerHandler extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    ctx.writeAndFlush(msg)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}

object EchoServerHandler extends Server {
  override def pipeline(): List[ChannelHandler] = List(new EchoServerHandler)
}
