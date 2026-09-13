package net.minecraft.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.IdDispatchCodec;

public class SkipPacketDecoderException extends DecoderException implements IdDispatchCodec.DontDecorateException, SkipPacketException {
   public SkipPacketDecoderException(String p_393826_) {
      super(p_393826_);
   }

   public SkipPacketDecoderException(Throwable p_392147_) {
      super(p_392147_);
   }
}
