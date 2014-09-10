package com.github.dcapwell.netty.examples.block.v2

import com.google.common.primitives.{Ints, Longs}

case class RequestHeader(version: Version, tpe: RequestType.RequestType)

object RequestType extends Enumeration {
  type RequestType = Value
  val GetBlock, PutBlock = Value
}

sealed trait Request extends Any

object Request {
  val HeaderSize = Longs.BYTES + Ints.BYTES
  val PutSize = Longs.BYTES
  val PacketHeaderSize = 2 * Longs.BYTES + Ints.BYTES + 1 // boolean
  val GetBlockSize = 2 * Ints.BYTES
}

case class GetBlock(blockId: BlockId, offset: Option[Int], length: Option[Int]) extends Request

case class PutBlock(blockId: BlockId) extends Request

case class PacketHeader(blockOffset: Long, sequenceNum: Long, length: Int, last: Boolean)

case class PutPacket(header: PacketHeader, data: Array[Byte]) extends Request

object ResponseType extends Enumeration {
  type ResponseType = Value
  val GetBlockResponse, PutBlockSuccess, BlockNotFound = Value
}

case class ResponseHeader(tpe: ResponseType.ResponseType)

sealed trait Response extends Any

case class GetBlockResponse(blockId: BlockId, length: Int, data: Array[Byte]) extends Response

case class PutBlockSuccess(blockId: BlockId) extends Response

// failure cases
case class BlockNotFound(blockId: BlockId) extends Response

case class OutOfOrder(blockId: BlockId, msg: String) extends Response
