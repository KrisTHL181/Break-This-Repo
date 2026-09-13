package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class EntityBasedExplosionDamageCalculator extends ExplosionDamageCalculator {
   private final Entity source;

   public EntityBasedExplosionDamageCalculator(final Entity source) {
      this.source = source;
   }

   @Override
   public Optional<Float> getBlockExplosionResistance(
      final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState block, final FluidState fluid
   ) {
      return super.getBlockExplosionResistance(explosion, level, pos, block, fluid)
         .map(resistance -> this.source.getBlockExplosionResistance(explosion, level, pos, block, fluid, resistance));
   }

   @Override
   public boolean shouldBlockExplode(final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState state, final float power) {
      return this.source.shouldBlockExplode(explosion, level, pos, state, power);
   }
}
