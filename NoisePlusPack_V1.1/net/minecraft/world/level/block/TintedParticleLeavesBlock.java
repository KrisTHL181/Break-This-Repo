package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class TintedParticleLeavesBlock extends LeavesBlock {
   public static final MapCodec<TintedParticleLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422130_ -> p_422130_.group(
            ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("leaf_particle_chance").forGetter(p_393870_ -> p_393870_.leafParticleChance), propertiesCodec()
         )
         .apply(p_422130_, TintedParticleLeavesBlock::new)
   );

   public TintedParticleLeavesBlock(float p_392052_, BlockBehaviour.Properties p_391381_) {
      super(p_392052_, p_391381_);
   }

   @Override
   protected void spawnFallingLeavesParticle(Level p_391521_, BlockPos p_397831_, RandomSource p_393219_) {
      ColorParticleOption colorparticleoption = ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, p_391521_.getClientLeafTintColor(p_397831_));
      ParticleUtils.spawnParticleBelow(p_391521_, p_397831_, p_393219_, colorparticleoption);
   }

   @Override
   public MapCodec<? extends TintedParticleLeavesBlock> codec() {
      return CODEC;
   }
}
