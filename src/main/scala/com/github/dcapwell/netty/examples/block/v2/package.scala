package com.github.dcapwell.netty.examples.block

package object v2 {
  case class Version(value: Long) extends AnyVal

  val CurrentVersion = Version(1)

  case class Size(value: Int) extends AnyVal

  case class BlockId(value: Long) extends AnyVal
}
