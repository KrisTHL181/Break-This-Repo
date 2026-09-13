package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;

public class SpellParticleOption implements ParticleOptions {
   private final ParticleType<SpellParticleOption> type;
   private final int color;
   private final float power;

   public static MapCodec<SpellParticleOption> codec(ParticleType<SpellParticleOption> p_427901_) {
      return RecordCodecBuilder.mapCodec(
         p_422371_ -> p_422371_.group(
               ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color", -1).forGetter(p_425023_ -> p_425023_.color),
               Codec.FLOAT.optionalFieldOf("power", 1.0F).forGetter(p_423840_ -> p_423840_.power)
            )
            .apply(p_422371_, (p_425939_, p_423139_) -> new SpellParticleOption(p_427901_, p_425939_, p_423139_))
      );
   }

   public static StreamCodec<? super ByteBuf, SpellParticleOption> streamCodec(ParticleType<SpellParticleOption> p_429628_) {
      return StreamCodec.composite(
         ByteBufCodecs.INT,
         p_429922_ -> p_429922_.color,
         ByteBufCodecs.FLOAT,
         p_430707_ -> p_430707_.power,
         (p_429068_, p_423570_) -> new SpellParticleOption(p_429628_, p_429068_, p_423570_)
      );
   }

   private SpellParticleOption(ParticleType<SpellParticleOption> p_429909_, int p_426146_, float p_427781_) {
      this.type = p_429909_;
      this.color = p_426146_;
      this.power = p_427781_;
   }

   @Override
   public ParticleType<SpellParticleOption> getType() {
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

   public float getPower() {
      return this.power;
   }

   public static SpellParticleOption create(ParticleType<SpellParticleOption> p_430449_, int p_424734_, float p_428214_) {
      return new SpellParticleOption(p_430449_, p_424734_, p_428214_);
   }

   public static SpellParticleOption create(ParticleType<SpellParticleOption> p_424428_, float p_427471_, float p_425792_, float p_427970_, float p_422529_) {
      return create(p_424428_, ARGB.colorFromFloat(1.0F, p_427471_, p_425792_, p_427970_), p_422529_);
   }
}
