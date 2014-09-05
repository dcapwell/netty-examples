package com.github.dcapwell.netty.examples.block

import com.google.common.primitives.{Ints, Longs}

trait Message extends AnyRef

object Message {
  val GetBlockSize = Longs.BYTES + Ints.BYTES * 2

  def wrap(value: Int): Option[Int] =
    if (value < 0) None
    else Some(value)

  def unwrap(value: Option[Int]): Int =
    value.getOrElse(-1)
}

case class GetBlock(blockId: BlockId, offset: Option[Int], length: Option[Int]) extends Message

case class PutBlock(blockId: BlockId, data: Array[Byte]) extends Message

object MessageType extends Enumeration {
  type MessageType = Value
  val Get, Put = Value
}