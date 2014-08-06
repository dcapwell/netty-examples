package io.pivotal.netty.examples

import java.util.Date

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelFuture, ChannelHandler, ChannelInboundHandlerAdapter, ChannelHandlerContext}
import io.netty.handler.codec.{MessageToByteEncoder, ByteToMessageDecoder}

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

object UnixTimeEncoder extends MessageToByteEncoder[UnixTime] {
  override def encode(ctx: ChannelHandlerContext, msg: UnixTime, out: ByteBuf): Unit = {
    println(s"Encoding message $msg")
    out.writeInt(msg.value.toInt)
  }
}

class UnixTimeClientHandler extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    val m = msg.asInstanceOf[UnixTime]
    println(m)
    ctx.close()
  }
}

object UnixTimeClient extends Client {
  override def port: Int = 55297

  override def pipeline: List[ChannelHandler] = List(new UnixTimeDecoder, new UnixTimeClientHandler)
}

object UnixTimeServerHandler extends ChannelInboundHandlerAdapter {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    val data = new UnixTime(System.currentTimeMillis())
    println(s"Writing data $data")
    val future = ctx.writeAndFlush(data)

    future.addListener((f: ChannelFuture) => {println("closing"); ctx.close()})
  }


  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}

object UnixTimeServer extends Server {
  override def pipeline(): List[ChannelHandler] =
    List(UnixTimeEncoder, UnixTimeServerHandler)
}
