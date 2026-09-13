package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class ShriekParticleOption implements ParticleOptions {
   public static final MapCodec<ShriekParticleOption> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.INT.fieldOf("delay").forGetter(o -> o.delay)).apply(i, ShriekParticleOption::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ShriekParticleOption> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, o -> o.delay, ShriekParticleOption::new
   );
   private final int delay;

   public ShriekParticleOption(final int delay) {
      this.delay = delay;
   }

   @Override
   public ParticleType<ShriekParticleOption> getType() {
      return ParticleTypes.SHRIEK;
   }

   public int getDelay() {
      return this.delay;
   }
}
