package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public record AmbientParticle(ParticleOptions particle, float probability) {
   public static final Codec<AmbientParticle> CODEC = RecordCodecBuilder.create(
      p_450236_ -> p_450236_.group(
            ParticleTypes.CODEC.fieldOf("particle").forGetter(p_455989_ -> p_455989_.particle),
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(p_460768_ -> p_460768_.probability)
         )
         .apply(p_450236_, AmbientParticle::new)
   );

   public boolean canSpawn(RandomSource p_454173_) {
      return p_454173_.nextFloat() <= this.probability;
   }

   public static List<AmbientParticle> of(ParticleOptions p_457381_, float p_454127_) {
      return List.of(new AmbientParticle(p_457381_, p_454127_));
   }
}
