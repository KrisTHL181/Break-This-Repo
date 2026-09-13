package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class PowerParticleOption implements ParticleOptions {
   private final ParticleType<PowerParticleOption> type;
   private final float power;

   public static MapCodec<PowerParticleOption> codec(ParticleType<PowerParticleOption> p_423521_) {
      return Codec.FLOAT
         .xmap(p_424842_ -> new PowerParticleOption(p_423521_, p_424842_), p_430579_ -> p_430579_.power)
         .optionalFieldOf("power", create(p_423521_, 1.0F));
   }

   public static StreamCodec<? super ByteBuf, PowerParticleOption> streamCodec(ParticleType<PowerParticleOption> p_422514_) {
      return ByteBufCodecs.FLOAT.map(p_426690_ -> new PowerParticleOption(p_422514_, p_426690_), p_423974_ -> p_423974_.power);
   }

   private PowerParticleOption(ParticleType<PowerParticleOption> p_426735_, float p_423165_) {
      this.type = p_426735_;
      this.power = p_423165_;
   }

   @Override
   public ParticleType<PowerParticleOption> getType() {
      return this.type;
   }

   public float getPower() {
      return this.power;
   }

   public static PowerParticleOption create(ParticleType<PowerParticleOption> p_425037_, float p_428381_) {
      return new PowerParticleOption(p_425037_, p_428381_);
   }
}
