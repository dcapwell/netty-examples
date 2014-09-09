package com.github.dcapwell.netty.examples.block

import java.util

import com.github.dcapwell.netty.examples.Client
import com.google.common.base.{Charsets, Strings}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel._
import io.netty.handler.codec.{MessageToByteEncoder, MessageToMessageEncoder}

object BlockClient extends App {
  lazy val Port = 60389

  new Client {
    override def port: Int = Port

    override def pipeline: List[ChannelHandler] = List(
      new HeaderEncoder,
      new MessageEncoder,
      new RequestEncoder,
      new PutWorker
    )
  }
  Thread.sleep(100)

  new Client {
    override def port: Int = Port

    override def pipeline: List[ChannelHandler] = List(
      new HeaderEncoder,
      new MessageEncoder,
      new RequestEncoder,
      new GetWorker
    )
  }
}

abstract class PrintReader extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    val in = msg.asInstanceOf[ByteBuf]
    if (in.readableBytes() > 4) {
      println(s"Version: ${in.readLong()}")
      println(s"Type: ${MessageResponseType(in.readInt())}")
      println(s"Size: ${in.readInt()}")
      println(s"BlockId: ${in.readLong()}")
      val data = in.toString(Charsets.UTF_8)
      val expected = DataGenerator.generateRaw
      println(s"Data:     $data")
      println(s"Expected: $expected")
      if (data.equals(expected)) {
        throw new AssertionError("Does not match!")
      }
      ctx.close()
    }
  }
}

class GetWorker extends PrintReader {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.writeAndFlush(get(BlockId(10)))
  }

  private[this] def get(blockId: BlockId): Request = {
    val msg = GetBlock(blockId, None, None)
    val header = RequestHeader(CurrentVersion, MessageType.Get, Size(Message.GetBlockSize))
    Request(header, msg)
  }
}

object DataGenerator {
  def generate(): Array[Byte] =
    generateRaw.getBytes(Charsets.UTF_8)

  val generateRaw: String =
    Strings.repeat("Foo Bar Baz!", 1000000)
}

class PutWorker extends PrintReader {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.writeAndFlush(put(BlockId(10), DataGenerator.generate()))
  }

  private[this] def put(blockId: BlockId, data: Array[Byte]): Request = {
    val msg = PutBlock(blockId, data)
    val header = RequestHeader(CurrentVersion, MessageType.Put, Size(Message.putSize(data)))
    Request(header, msg)
  }
}

class MessageEncoder extends MessageToByteEncoder[Message] {
  override def encode(ctx: ChannelHandlerContext, msg: Message, out: ByteBuf): Unit = msg match {
    case GetBlock(blockId, offset, length) =>
      blockId.write(out)
      out.writeInt(Message.unwrap(offset))
      out.writeInt(Message.unwrap(length))

    case PutBlock(blockId, data) =>
      blockId.write(out)
      out.writeBytes(Unpooled.wrappedBuffer(data))
  }
}

class HeaderEncoder extends MessageToByteEncoder[RequestHeader] {
  override def encode(ctx: ChannelHandlerContext, msg: RequestHeader, out: ByteBuf): Unit = {
    out.writeLong(msg.version.value)
    out.writeInt(msg.messageType.id)
    out.writeInt(msg.messageSize.value)
  }
}

class RequestEncoder extends MessageToMessageEncoder[Request] {
  override def encode(ctx: ChannelHandlerContext, msg: Request, out: util.List[AnyRef]): Unit = {
    out.add(msg.header)
    out.add(msg.message)
  }
}