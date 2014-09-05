package com.github.dcapwell.netty.examples.block

trait BlockStore {
   def apply(blockId: BlockId): Either[BlockNotFound, Array[Byte]]

  def add(blockId: BlockId, value: Array[Byte]): Unit
 }
