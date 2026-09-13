package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import javax.crypto.Cipher;

public class CipherDecoder extends MessageToMessageDecoder<ByteBuf> {
   private final CipherBase cipher;

   public CipherDecoder(final Cipher cipher) {
      this.cipher = new CipherBase(cipher);
   }

   protected void decode(final ChannelHandlerContext ctx, final ByteBuf msg, final List<Object> out) throws Exception {
      out.add(this.cipher.decipher(ctx, msg));
   }
}
