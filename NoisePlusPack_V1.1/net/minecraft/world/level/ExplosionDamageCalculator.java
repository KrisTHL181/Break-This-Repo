package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class ExplosionDamageCalculator {
   public Optional<Float> getBlockExplosionResistance(Explosion p_46099_, BlockGetter p_46100_, BlockPos p_46101_, BlockState p_46102_, FluidState p_46103_) {
      return p_46102_.isAir() && p_46103_.isEmpty()
         ? Optional.empty()
         : Optional.of(Math.max(p_46102_.getBlock().getExplosionResistance(), p_46103_.getExplosionResistance()));
   }

   public boolean shouldBlockExplode(Explosion p_46094_, BlockGetter p_46095_, BlockPos p_46096_, BlockState p_46097_, float p_46098_) {
      return true;
   }

   public boolean shouldDamageEntity(Explosion p_312772_, Entity p_311132_) {
      return true;
   }

   public float getKnockbackMultiplier(Entity p_330296_) {
      return 1.0F;
   }

   public float getEntityDamageAmount(Explosion p_310428_, Entity p_310135_, float p_365084_) {
      float f = p_310428_.radius() * 2.0F;
      Vec3 vec3 = p_310428_.center();
      double d0 = Math.sqrt(p_310135_.distanceToSqr(vec3)) / f;
      double d1 = (1.0 - d0) * p_365084_;
      return (float)((d1 * d1 + d1) / 2.0 * 7.0 * f + 1.0);
   }
}
