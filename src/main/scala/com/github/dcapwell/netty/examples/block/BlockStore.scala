package com.github.dcapwell.netty.examples.block

trait BlockStore[Key] {
  def apply(key: Key): Option[Array[Byte]]

  def add(key: Key, value: Array[Byte]): Unit
}
