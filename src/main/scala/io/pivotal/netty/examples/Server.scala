package io.pivotal.netty.examples

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelHandler, ChannelOption, ChannelInitializer}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

trait Server extends App {
  val port = 0

  val bossGroup = new NioEventLoopGroup()
  val workerGroup = new NioEventLoopGroup()

  try {
    val childHandler = new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit =
        ch.pipeline().addLast(handler())
    }

    val bootstrap = new ServerBootstrap().
      group(bossGroup, workerGroup).
      channel(classOf[NioServerSocketChannel]).
      childHandler(childHandler).
      option(ChannelOption.SO_BACKLOG, new Integer(128)).
      childOption(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)

    val future = bootstrap.bind(port).sync()
    println(s"Connected at: ${future.channel().localAddress()}")

    //    future.channel().close().sync()
    while (true) {
      import scala.concurrent.duration._
      Thread.sleep(5.seconds.toMillis)
    }
  } finally {
    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
  }

  def handler(): ChannelHandler
}
