package net.minecraft.server.rcon;

import java.nio.charset.StandardCharsets;

public class PktUtils {
   public static final int MAX_PACKET_SIZE = 1460;
   public static final char[] HEX_CHAR = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

   public static String stringFromByteArray(final byte[] b, final int offset, final int length) {
      int max = length - 1;
      int i = offset > max ? max : offset;

      while (0 != b[i] && i < max) {
         i++;
      }

      return new String(b, offset, i - offset, StandardCharsets.UTF_8);
   }

   public static int intFromByteArray(final byte[] b, final int offset) {
      return intFromByteArray(b, offset, b.length);
   }

   public static int intFromByteArray(final byte[] b, final int offset, final int length) {
      return 0 > length - offset - 4 ? 0 : b[offset + 3] << 24 | (b[offset + 2] & 0xFF) << 16 | (b[offset + 1] & 0xFF) << 8 | b[offset] & 0xFF;
   }

   public static int intFromNetworkByteArray(final byte[] b, final int offset, final int length) {
      return 0 > length - offset - 4 ? 0 : b[offset] << 24 | (b[offset + 1] & 0xFF) << 16 | (b[offset + 2] & 0xFF) << 8 | b[offset + 3] & 0xFF;
   }

   public static String toHexString(final byte b) {
      return "" + HEX_CHAR[(b & 240) >>> 4] + HEX_CHAR[b & 15];
   }
}
