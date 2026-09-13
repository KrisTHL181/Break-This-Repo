package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import javax.crypto.Cipher;

public class CipherEncoder extends MessageToByteEncoder<ByteBuf> {
   private final CipherBase cipher;

   public CipherEncoder(final Cipher cipher) {
      this.cipher = new CipherBase(cipher);
   }

   protected void encode(final ChannelHandlerContext ctx, final ByteBuf msg, final ByteBuf out) throws Exception {
      this.cipher.encipher(msg, out);
   }
}
