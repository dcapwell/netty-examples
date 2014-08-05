package io.pivotal.netty.examples

import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandler, ChannelInitializer, ChannelOption, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel

trait Client extends App {
  def port: Int
  def pipeline: List[ChannelHandler]

  def hostname: String = "localhost"

  val workerGroup: EventLoopGroup = new NioEventLoopGroup()
  try {
    val b = new Bootstrap()
    b.group(workerGroup)
    b.channel(classOf[NioSocketChannel])
    b.option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)
    b.handler(new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit =
        ch.pipeline().addLast(pipeline: _*)
    })

    // Start the client.
    val f = b.connect(hostname, port).sync()

    // Wait until the connection is closed.
    f.channel().closeFuture().sync()
  } finally {
    workerGroup.shutdownGracefully()
  }
}
