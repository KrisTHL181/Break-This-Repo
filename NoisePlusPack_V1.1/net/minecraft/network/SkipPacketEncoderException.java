package net.minecraft.network;

import io.netty.handler.codec.EncoderException;
import net.minecraft.network.codec.IdDispatchCodec;

public class SkipPacketEncoderException extends EncoderException implements IdDispatchCodec.DontDecorateException, SkipPacketException {
   public SkipPacketEncoderException(String p_396598_) {
      super(p_396598_);
   }

   public SkipPacketEncoderException(Throwable p_393577_) {
      super(p_393577_);
   }
}
