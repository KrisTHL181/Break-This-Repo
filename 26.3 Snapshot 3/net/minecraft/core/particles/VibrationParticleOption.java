package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;

public class VibrationParticleOption implements ParticleOptions {
   private static final Codec<PositionSource> SAFE_POSITION_SOURCE_CODEC = PositionSource.CODEC
      .validate(e -> e instanceof EntityPositionSource ? DataResult.error(() -> "Entity position sources are not allowed") : DataResult.success(e));
   public static final MapCodec<VibrationParticleOption> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            SAFE_POSITION_SOURCE_CODEC.fieldOf("destination").forGetter(VibrationParticleOption::getDestination),
            Codec.INT.fieldOf("arrival_in_ticks").forGetter(VibrationParticleOption::getArrivalInTicks)
         )
         .apply(i, VibrationParticleOption::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, VibrationParticleOption> STREAM_CODEC = StreamCodec.composite(
      PositionSource.STREAM_CODEC,
      VibrationParticleOption::getDestination,
      ByteBufCodecs.VAR_INT,
      VibrationParticleOption::getArrivalInTicks,
      VibrationParticleOption::new
   );
   private final PositionSource destination;
   private final int arrivalInTicks;

   public VibrationParticleOption(final PositionSource destination, final int arrivalInTicks) {
      this.destination = destination;
      this.arrivalInTicks = arrivalInTicks;
   }

   @Override
   public ParticleType<VibrationParticleOption> getType() {
      return ParticleTypes.VIBRATION;
   }

   public PositionSource getDestination() {
      return this.destination;
   }

   public int getArrivalInTicks() {
      return this.arrivalInTicks;
   }
}
