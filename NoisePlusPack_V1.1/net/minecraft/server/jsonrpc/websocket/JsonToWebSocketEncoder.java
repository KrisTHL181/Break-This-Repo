package net.minecraft.server.jsonrpc.websocket;

import com.google.gson.JsonElement;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.List;

public class JsonToWebSocketEncoder extends MessageToMessageEncoder<JsonElement> {
   protected void encode(ChannelHandlerContext p_422412_, JsonElement p_424511_, List<Object> p_425140_) {
      p_425140_.add(new TextWebSocketFrame(p_424511_.toString()));
   }
}
