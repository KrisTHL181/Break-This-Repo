package net.minecraft.server.jsonrpc.websocket;

import com.google.gson.JsonElement;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.List;

public class JsonToWebSocketEncoder extends MessageToMessageEncoder<JsonElement> {
   protected void encode(final ChannelHandlerContext ctx, final JsonElement msg, final List<Object> out) {
      out.add(new TextWebSocketFrame(msg.toString()));
   }
}
