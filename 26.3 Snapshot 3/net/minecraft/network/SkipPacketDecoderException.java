package net.minecraft.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.IdDispatchCodec;

public class SkipPacketDecoderException extends DecoderException implements IdDispatchCodec.DontDecorateException, SkipPacketException {
   public SkipPacketDecoderException(final String message) {
      super(message);
   }

   public SkipPacketDecoderException(final Throwable cause) {
      super(cause);
   }
}
