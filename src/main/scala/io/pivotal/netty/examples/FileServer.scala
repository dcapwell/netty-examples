package io.pivotal.netty.examples

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelFuture, ChannelHandler, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.{MessageToByteEncoder, MessageToMessageDecoder}
import io.netty.handler.codec.string.StringDecoder
import io.netty.util.CharsetUtil

case class FileRequest(path: String)

case class FileResponse(path: String, length: Long, data: ByteBuffer)

object FileRequestDecoder extends MessageToMessageDecoder[String] {
  override def decode(ctx: ChannelHandlerContext, in: String, out: util.List[AnyRef]): Unit = {
    out.add(FileRequest(in))
  }
}

object FileResponseEncoder extends MessageToByteEncoder[FileResponse] {
  private[this] val Encoding = CharsetUtil.UTF_8

  override def encode(ctx: ChannelHandlerContext, msg: FileResponse, out: ByteBuf): Unit = {
    println(s"Encoding path ${msg.path}")
    writeString(out, msg.path)
    out.writeLong(msg.length)
    out.writeBytes(msg.data)
  }

  private def writeString(out: ByteBuf, data: String) = {
    out.writeLong(data.length)
    out.writeBytes(data.getBytes(Encoding))
  }
}

object FileServerHandler extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    val req = msg.asInstanceOf[FileRequest]
    val file: RandomAccessFile = new RandomAccessFile(req.path.trim, "r")
    val length = file.length()
    val channel = file.getChannel()
    val data = channel.map(FileChannel.MapMode.READ_ONLY, 0, length)

    println(s"Serving path ${req.path}")
    val future = ctx.writeAndFlush(FileResponse(req.path, length, data))

    future.addListener((f: ChannelFuture) => ctx.close())
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}

object FileServer extends Server {
  override def pipeline(): List[ChannelHandler] =
    List(
      new StringDecoder(CharsetUtil.UTF_8),
      FileRequestDecoder,
      FileResponseEncoder,
      FileServerHandler
    )
}
