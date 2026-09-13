package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public record AmbientParticle(ParticleOptions particle, float probability) {
   public static final Codec<AmbientParticle> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ParticleTypes.CODEC.fieldOf("particle").forGetter(s -> s.particle),
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(s -> s.probability)
         )
         .apply(i, AmbientParticle::new)
   );

   public boolean canSpawn(final RandomSource random) {
      return random.nextFloat() <= this.probability;
   }

   public static List<AmbientParticle> of(final ParticleOptions particle, final float probability) {
      return List.of(new AmbientParticle(particle, probability));
   }
}
