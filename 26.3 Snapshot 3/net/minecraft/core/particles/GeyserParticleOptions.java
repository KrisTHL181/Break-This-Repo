package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record GeyserParticleOptions(ParticleType<GeyserParticleOptions> type, int waterBlocks) implements ParticleOptions {
   public static MapCodec<GeyserParticleOptions> codec(final ParticleType<GeyserParticleOptions> type) {
      return RecordCodecBuilder.mapCodec(
         i -> i.group(ExtraCodecs.POSITIVE_INT.fieldOf("water_blocks").forGetter(o -> o.waterBlocks))
            .apply(i, waterBlocks -> new GeyserParticleOptions(type, waterBlocks))
      );
   }

   public static StreamCodec<? super ByteBuf, GeyserParticleOptions> streamCodec(final ParticleType<GeyserParticleOptions> type) {
      return StreamCodec.composite(ByteBufCodecs.INT, o -> o.waterBlocks, waterBlocks -> new GeyserParticleOptions(type, waterBlocks));
   }

   @Override
   public ParticleType<GeyserParticleOptions> getType() {
      return this.type;
   }
}
