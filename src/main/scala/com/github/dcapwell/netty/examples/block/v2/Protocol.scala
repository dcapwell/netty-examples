package com.github.dcapwell.netty.examples.block.v2

import com.google.common.primitives.{Ints, Longs}

case class RequestHeader(version: Version, tpe: RequestType.RequestType, blockId: BlockId)

object RequestType extends Enumeration {
  type RequestType = Value
  val GetBlock, PutBlock = Value
}

sealed trait Request extends Any

object Request {
  val HeaderSize = Longs.BYTES + Ints.BYTES
  val PacketHeaderSize = 2 * Longs.BYTES + Ints.BYTES + 1 // boolean
}

case class GetBlock(offset: Option[Int], length: Option[Int]) extends Request

case class PacketHeader(blockOffset: Long, sequenceNum: Long, length: Int, last: Boolean)

case class PutPacket(header: PacketHeader, data: Array[Byte]) extends Request

object ResponseType extends Enumeration {
  type ResponseType = Value
  val GetBlockResponse, PutBlockSuccess, BlockNotFound = Value
}

case class ResponseHeader(tpe: ResponseType.ResponseType, blockId: BlockId)

sealed trait Response extends Any

case class GetBlockResponse(blockId: BlockId, data: Array[Byte]) extends Response

case class PutBlockSuccess(blockId: BlockId) extends Response

// failure cases
case class BlockNotFound(blockId: BlockId) extends Response
