package net.minecraft.server.network;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public class LegacyProtocolUtils {
   public static final int CUSTOM_PAYLOAD_PACKET_ID = 250;
   public static final String CUSTOM_PAYLOAD_PACKET_PING_CHANNEL = "MC|PingHost";
   public static final int GET_INFO_PACKET_ID = 254;
   public static final int GET_INFO_PACKET_VERSION_1 = 1;
   public static final int DISCONNECT_PACKET_ID = 255;
   public static final int FAKE_PROTOCOL_VERSION = 127;

   public static void writeLegacyString(ByteBuf p_301020_, String p_300839_) {
      p_301020_.writeShort(p_300839_.length());
      p_301020_.writeCharSequence(p_300839_, StandardCharsets.UTF_16BE);
   }

   public static String readLegacyString(ByteBuf p_297756_) {
      int i = p_297756_.readShort();
      int j = i * 2;
      String s = p_297756_.toString(p_297756_.readerIndex(), j, StandardCharsets.UTF_16BE);
      p_297756_.skipBytes(j);
      return s;
   }
}
