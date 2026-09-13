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

   public static MapCodec<ColorParticleOption> codec(ParticleType<ColorParticleOption> p_329922_) {
      return ExtraCodecs.ARGB_COLOR_CODEC.xmap(p_335886_ -> new ColorParticleOption(p_329922_, p_335886_), p_328917_ -> p_328917_.color).fieldOf("color");
   }

   public static StreamCodec<? super ByteBuf, ColorParticleOption> streamCodec(ParticleType<ColorParticleOption> p_328683_) {
      return ByteBufCodecs.INT.map(p_330079_ -> new ColorParticleOption(p_328683_, p_330079_), p_329364_ -> p_329364_.color);
   }

   private ColorParticleOption(ParticleType<ColorParticleOption> p_330442_, int p_329966_) {
      this.type = p_330442_;
      this.color = p_329966_;
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

   public static ColorParticleOption create(ParticleType<ColorParticleOption> p_329254_, int p_327671_) {
      return new ColorParticleOption(p_329254_, p_327671_);
   }

   public static ColorParticleOption create(ParticleType<ColorParticleOption> p_328973_, float p_334118_, float p_330068_, float p_330217_) {
      return create(p_328973_, ARGB.colorFromFloat(1.0F, p_334118_, p_330068_, p_330217_));
   }
}
