package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record GeyserBaseParticleOptions(ParticleType<GeyserBaseParticleOptions> type, int waterBlocks, float burstImpulseBase) implements ParticleOptions {
   public static MapCodec<GeyserBaseParticleOptions> codec(final ParticleType<GeyserBaseParticleOptions> type) {
      return RecordCodecBuilder.mapCodec(
         i -> i.group(
               ExtraCodecs.POSITIVE_INT.fieldOf("water_blocks").forGetter(o -> o.waterBlocks),
               Codec.FLOAT.fieldOf("burst_impulse_base").forGetter(o -> o.burstImpulseBase)
            )
            .apply(i, (waterBlocks, burstImpulseBase) -> new GeyserBaseParticleOptions(type, waterBlocks, burstImpulseBase))
      );
   }

   public static StreamCodec<? super ByteBuf, GeyserBaseParticleOptions> streamCodec(final ParticleType<GeyserBaseParticleOptions> type) {
      return StreamCodec.composite(
         ByteBufCodecs.INT,
         o -> o.waterBlocks,
         ByteBufCodecs.FLOAT,
         o -> o.burstImpulseBase,
         (waterBlocks, burstImpulseBase) -> new GeyserBaseParticleOptions(type, waterBlocks, burstImpulseBase)
      );
   }

   @Override
   public ParticleType<GeyserBaseParticleOptions> getType() {
      return this.type;
   }
}
