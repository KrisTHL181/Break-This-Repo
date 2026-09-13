package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;

public record TrailParticleOption(Vec3 target, int color, int duration) implements ParticleOptions {
   public static final MapCodec<TrailParticleOption> CODEC = RecordCodecBuilder.mapCodec(
      p_375832_ -> p_375832_.group(
            Vec3.CODEC.fieldOf("target").forGetter(TrailParticleOption::target),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(TrailParticleOption::color),
            ExtraCodecs.POSITIVE_INT.fieldOf("duration").forGetter(TrailParticleOption::duration)
         )
         .apply(p_375832_, TrailParticleOption::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TrailParticleOption> STREAM_CODEC = StreamCodec.composite(
      Vec3.STREAM_CODEC,
      TrailParticleOption::target,
      ByteBufCodecs.INT,
      TrailParticleOption::color,
      ByteBufCodecs.VAR_INT,
      TrailParticleOption::duration,
      TrailParticleOption::new
   );

   @Override
   public ParticleType<TrailParticleOption> getType() {
      return ParticleTypes.TRAIL;
   }
}
