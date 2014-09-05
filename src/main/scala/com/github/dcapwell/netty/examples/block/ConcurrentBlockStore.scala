package com.github.dcapwell.netty.examples.block

import java.util.concurrent.ConcurrentHashMap

import scala.collection.mutable

class ConcurrentBlockStore extends BlockStore {

  import scala.collection.convert.WrapAsScala._

  val data: mutable.ConcurrentMap[BlockId, Array[Byte]] = new ConcurrentHashMap[BlockId, Array[Byte]]

  override def apply(blockId: BlockId): Either[BlockNotFound, Array[Byte]] = {
    val result = data.get(blockId)
    result.toRight(BlockNotFound(blockId))
  }

  override def add(blockId: BlockId, value: Array[Byte]): Unit =
    data.put(blockId, value)
}
