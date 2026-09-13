package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SimpleExplosionDamageCalculator extends ExplosionDamageCalculator {
   private final boolean explodesBlocks;
   private final boolean damagesEntities;
   private final Optional<Float> knockbackMultiplier;
   private final Optional<HolderSet<Block>> immuneBlocks;

   public SimpleExplosionDamageCalculator(boolean p_344116_, boolean p_343987_, Optional<Float> p_342644_, Optional<HolderSet<Block>> p_343081_) {
      this.explodesBlocks = p_344116_;
      this.damagesEntities = p_343987_;
      this.knockbackMultiplier = p_342644_;
      this.immuneBlocks = p_343081_;
   }

   @Override
   public Optional<Float> getBlockExplosionResistance(
      Explosion p_344500_, BlockGetter p_342364_, BlockPos p_342845_, BlockState p_343076_, FluidState p_343209_
   ) {
      if (this.immuneBlocks.isPresent()) {
         return p_343076_.is(this.immuneBlocks.get()) ? Optional.of(3600000.0F) : Optional.empty();
      } else {
         return super.getBlockExplosionResistance(p_344500_, p_342364_, p_342845_, p_343076_, p_343209_);
      }
   }

   @Override
   public boolean shouldBlockExplode(Explosion p_344299_, BlockGetter p_344794_, BlockPos p_343238_, BlockState p_345107_, float p_343990_) {
      return this.explodesBlocks;
   }

   @Override
   public boolean shouldDamageEntity(Explosion p_342404_, Entity p_345509_) {
      return this.damagesEntities;
   }

   @Override
   public float getKnockbackMultiplier(Entity p_342532_) {
      boolean flag = p_342532_ instanceof Player player && player.getAbilities().flying;
      return flag ? 0.0F : this.knockbackMultiplier.orElseGet(() -> super.getKnockbackMultiplier(p_342532_));
   }
}
