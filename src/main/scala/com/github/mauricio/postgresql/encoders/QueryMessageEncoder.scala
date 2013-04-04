package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.{ChannelUtils, CharsetHelper}
import com.github.mauricio.postgresql.messages.frontend.{FrontendMessage, QueryMessage}
import com.github.mauricio.postgresql.messages.backend.Message
import java.nio.charset.Charset

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 8:32 PM
 */

class QueryMessageEncoder ( charset : Charset ) extends Encoder {

  override def encode(message: FrontendMessage): ChannelBuffer = {

    val m = message.asInstanceOf[QueryMessage]

    val buffer = ChannelBuffers.dynamicBuffer()
    buffer.writeByte( Message.Query )
    buffer.writeInt(0)
    ChannelUtils.writeCString(m.query, buffer, charset)

    ChannelUtils.writeLength(  buffer )

    buffer
  }

}