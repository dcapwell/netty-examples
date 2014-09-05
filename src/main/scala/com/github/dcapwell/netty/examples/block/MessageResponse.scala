package com.github.dcapwell.netty.examples.block

trait MessageResponse extends Any

case class GetBlockResponse(blockId: BlockId, data: Array[Byte]) extends MessageResponse

case class BlockNotFound(blockId: BlockId) extends MessageResponse

object MessageResponseType extends Enumeration {
  type MessageResponseType = Value
  val GetBlockResponse, BlockNotFound = Value
}
