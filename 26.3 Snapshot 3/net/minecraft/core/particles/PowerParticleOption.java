package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class PowerParticleOption implements ParticleOptions {
   private final ParticleType<PowerParticleOption> type;
   private final float power;

   public static MapCodec<PowerParticleOption> codec(final ParticleType<PowerParticleOption> type) {
      return Codec.FLOAT.xmap(power -> new PowerParticleOption(type, power), o -> o.power).optionalFieldOf("power", create(type, 1.0F));
   }

   public static StreamCodec<? super ByteBuf, PowerParticleOption> streamCodec(final ParticleType<PowerParticleOption> type) {
      return ByteBufCodecs.FLOAT.map(color -> new PowerParticleOption(type, color), o -> o.power);
   }

   private PowerParticleOption(final ParticleType<PowerParticleOption> type, final float power) {
      this.type = type;
      this.power = power;
   }

   @Override
   public ParticleType<PowerParticleOption> getType() {
      return this.type;
   }

   public float getPower() {
      return this.power;
   }

   public static PowerParticleOption create(final ParticleType<PowerParticleOption> type, final float power) {
      return new PowerParticleOption(type, power);
   }
}
