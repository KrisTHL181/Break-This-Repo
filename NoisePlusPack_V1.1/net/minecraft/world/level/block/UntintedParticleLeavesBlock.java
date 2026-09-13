package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class UntintedParticleLeavesBlock extends LeavesBlock {
   public static final MapCodec<UntintedParticleLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422134_ -> p_422134_.group(
            ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("leaf_particle_chance").forGetter(p_393506_ -> p_393506_.leafParticleChance),
            ParticleTypes.CODEC.fieldOf("leaf_particle").forGetter(p_393855_ -> p_393855_.leafParticle),
            propertiesCodec()
         )
         .apply(p_422134_, UntintedParticleLeavesBlock::new)
   );
   protected final ParticleOptions leafParticle;

   public UntintedParticleLeavesBlock(float p_397840_, ParticleOptions p_396382_, BlockBehaviour.Properties p_394856_) {
      super(p_397840_, p_394856_);
      this.leafParticle = p_396382_;
   }

   @Override
   protected void spawnFallingLeavesParticle(Level p_391353_, BlockPos p_393903_, RandomSource p_394927_) {
      ParticleUtils.spawnParticleBelow(p_391353_, p_393903_, p_394927_, this.leafParticle);
   }

   @Override
   public MapCodec<UntintedParticleLeavesBlock> codec() {
      return CODEC;
   }
}
