package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;

public class ColorParticleOption implements ParticleOptions {
   private final ParticleType<ColorParticleOption> type;
   private final int color;

   public static MapCodec<ColorParticleOption> codec(final ParticleType<ColorParticleOption> type) {
      return ExtraCodecs.ARGB_COLOR_CODEC.xmap(color -> new ColorParticleOption(type, color), o -> o.color).fieldOf("color");
   }

   public static StreamCodec<? super ByteBuf, ColorParticleOption> streamCodec(final ParticleType<ColorParticleOption> type) {
      return ByteBufCodecs.INT.map(color -> new ColorParticleOption(type, color), o -> o.color);
   }

   private ColorParticleOption(final ParticleType<ColorParticleOption> type, final int color) {
      this.type = type;
      this.color = color;
   }

   @Override
   public ParticleType<ColorParticleOption> getType() {
      return this.type;
   }

   public float getRed() {
      return ARGB.red(this.color) / 255.0F;
   }

   public float getGreen() {
      return ARGB.green(this.color) / 255.0F;
   }

   public float getBlue() {
      return ARGB.blue(this.color) / 255.0F;
   }

   public float getAlpha() {
      return ARGB.alpha(this.color) / 255.0F;
   }

   public static ColorParticleOption create(final ParticleType<ColorParticleOption> type, final int color) {
      return new ColorParticleOption(type, color);
   }

   public static ColorParticleOption create(final ParticleType<ColorParticleOption> type, final float red, final float green, final float blue) {
      return create(type, ARGB.colorFromFloat(1.0F, red, green, blue));
   }
}
