package net.minecraft.server.jsonrpc.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.List;

public class WebSocketToJsonCodec extends MessageToMessageDecoder<TextWebSocketFrame> {
   protected void decode(ChannelHandlerContext p_424022_, TextWebSocketFrame p_428947_, List<Object> p_427955_) {
      JsonElement jsonelement = JsonParser.parseString(p_428947_.text());
      p_427955_.add(jsonelement);
   }
}
