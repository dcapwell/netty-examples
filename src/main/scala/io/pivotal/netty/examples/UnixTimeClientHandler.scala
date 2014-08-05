package io.pivotal.netty.examples

import java.util.Date

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandler, ChannelInboundHandlerAdapter, ChannelHandlerContext}
import io.netty.handler.codec.ByteToMessageDecoder

case class UnixTime(value: Long) {
  override def toString: String = new Date(value).toString
}

object UnixTime {
  private[this] val TimeShift = 2208988800L

  def apply(value: Int): UnixTime = new UnixTime((value - TimeShift) * 1000L)
}

class UnixTimeDecoder extends ByteToMessageDecoder {
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: java.util.List[AnyRef]): Unit = {
    if (in.readableBytes() >= 4)
      out.add(UnixTime(in.readInt()))
  }
}

class UnixTimeClientHandler extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    val m = msg.asInstanceOf[UnixTime]
    println(m)
    ctx.close()
  }
}

object UnixTimeClientHandler extends Client {
  override def port: Int = 58071

  override def pipeline: List[ChannelHandler] = List(new UnixTimeDecoder, new UnixTimeClientHandler)
}
