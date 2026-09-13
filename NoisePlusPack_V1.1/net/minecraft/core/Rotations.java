package net.minecraft.core;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;

public record Rotations(float x, float y, float z) {
   public static final Codec<Rotations> CODEC = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         p_448579_ -> Util.fixedSize(p_448579_, 3).map(p_396301_ -> new Rotations((Float)p_396301_.get(0), (Float)p_396301_.get(1), (Float)p_396301_.get(2))),
         p_396757_ -> List.of(p_396757_.x(), p_396757_.y(), p_396757_.z())
      );
   public static final StreamCodec<ByteBuf, Rotations> STREAM_CODEC = new StreamCodec<ByteBuf, Rotations>() {
      public Rotations decode(ByteBuf p_335565_) {
         return new Rotations(p_335565_.readFloat(), p_335565_.readFloat(), p_335565_.readFloat());
      }

      public void encode(ByteBuf p_328300_, Rotations p_335839_) {
         p_328300_.writeFloat(p_335839_.x);
         p_328300_.writeFloat(p_335839_.y);
         p_328300_.writeFloat(p_335839_.z);
      }
   };

   public Rotations {
      x = !Float.isInfinite(x) && !Float.isNaN(x) ? x % 360.0F : 0.0F;
      y = !Float.isInfinite(y) && !Float.isNaN(y) ? y % 360.0F : 0.0F;
      z = !Float.isInfinite(z) && !Float.isNaN(z) ? z % 360.0F : 0.0F;
   }
}
