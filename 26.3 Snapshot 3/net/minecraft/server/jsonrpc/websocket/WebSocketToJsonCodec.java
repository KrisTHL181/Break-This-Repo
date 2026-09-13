package net.minecraft.server.jsonrpc.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.List;

public class WebSocketToJsonCodec extends MessageToMessageDecoder<TextWebSocketFrame> {
   protected void decode(final ChannelHandlerContext ctx, final TextWebSocketFrame msg, final List<Object> out) {
      String message = msg.text();
      JsonElement parsedMessage = JsonParser.parseString(message);
      out.add(parsedMessage);
   }
}
