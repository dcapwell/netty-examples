package io.pivotal.netty

import io.netty.channel.{ChannelFuture, ChannelFutureListener}

package object examples {

  implicit def FunctionToChannelFutureListener(fn: ChannelFuture => Any): ChannelFutureListener =
    new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture): Unit = fn(future)
    }

}
