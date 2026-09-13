package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Brightness(int block, int sky) {
   public static final Codec<Integer> LIGHT_VALUE_CODEC = ExtraCodecs.intRange(0, 15);
   public static final Codec<Brightness> CODEC = RecordCodecBuilder.create(
      i -> i.group(LIGHT_VALUE_CODEC.fieldOf("block").forGetter(Brightness::block), LIGHT_VALUE_CODEC.fieldOf("sky").forGetter(Brightness::sky))
         .apply(i, Brightness::new)
   );
   public static final Brightness FULL_BRIGHT = new Brightness(15, 15);

   public int pack() {
      return LightCoordsUtil.pack(this.block, this.sky);
   }

   public static Brightness unpack(final int packed) {
      return new Brightness(LightCoordsUtil.block(packed), LightCoordsUtil.sky(packed));
   }
}
