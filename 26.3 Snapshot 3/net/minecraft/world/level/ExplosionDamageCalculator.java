package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class ExplosionDamageCalculator {
   public Optional<Float> getBlockExplosionResistance(
      final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState block, final FluidState fluid
   ) {
      return block.isAir() && fluid.isEmpty()
         ? Optional.empty()
         : Optional.of(Math.max(block.getBlock().getExplosionResistance(), fluid.getExplosionResistance()));
   }

   public boolean shouldBlockExplode(final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState state, final float power) {
      return true;
   }

   public boolean shouldDamageEntity(final Explosion explosion, final Entity entity) {
      return true;
   }

   public float getKnockbackMultiplier(final Entity entity) {
      return 1.0F;
   }

   public float getEntityDamageAmount(final Explosion explosion, final Entity entity, final float exposure) {
      float doubleRadius = explosion.radius() * 2.0F;
      Vec3 center = explosion.center();
      double dist = Math.sqrt(entity.distanceToSqr(center)) / doubleRadius;
      double pow = (1.0 - dist) * exposure;
      return (float)((pow * pow + pow) / 2.0 * 7.0 * doubleRadius + 1.0);
   }
}
