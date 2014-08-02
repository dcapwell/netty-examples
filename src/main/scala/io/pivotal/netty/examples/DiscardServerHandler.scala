package io.pivotal.netty.examples

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer, ChannelOption}
import io.netty.util.ReferenceCountUtil

class DiscardServerHandler extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    // this is the same as the below.  Handlers deal with reference counted objects
    // so we need to clean them up ourselves

    //    msg.asInstanceOf[ByteBuf].release()
    try {
      // do nothing
    } finally ReferenceCountUtil.release(msg)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}

object DiscardServerHandler extends App {
  val port = 0

  val bossGroup = new NioEventLoopGroup()
  val workerGroup = new NioEventLoopGroup()

  try {
    val childHandler = new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit =
        ch.pipeline().addLast(new DiscardServerHandler)
    }

    val bootstrap = new ServerBootstrap().
      group(bossGroup, workerGroup).
      channel(classOf[NioServerSocketChannel]).
      childHandler(childHandler).
      option(ChannelOption.SO_BACKLOG, 128).
      childOption(ChannelOption.SO_KEEPALIVE, true)

    val future = bootstrap.bind(port).sync()

    future.channel().close().sync()
  } finally {
    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
  }
}