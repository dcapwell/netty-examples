package com.github.dcapwell.netty

import io.netty.channel.{ChannelFuture, ChannelFutureListener}

import scala.language.implicitConversions

package object examples {

  implicit def FunctionToChannelFutureListener(fn: ChannelFuture => Any): ChannelFutureListener =
    new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture): Unit = fn(future)
    }

}
