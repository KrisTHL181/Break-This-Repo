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

   public SimpleExplosionDamageCalculator(
      final boolean explodesBlocks, final boolean damagesEntities, final Optional<Float> knockbackMultiplier, final Optional<HolderSet<Block>> immuneBlocks
   ) {
      this.explodesBlocks = explodesBlocks;
      this.damagesEntities = damagesEntities;
      this.knockbackMultiplier = knockbackMultiplier;
      this.immuneBlocks = immuneBlocks;
   }

   @Override
   public Optional<Float> getBlockExplosionResistance(
      final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState block, final FluidState fluid
   ) {
      if (this.immuneBlocks.isPresent()) {
         return block.is(this.immuneBlocks.get()) ? Optional.of(3600000.0F) : Optional.empty();
      } else {
         return super.getBlockExplosionResistance(explosion, level, pos, block, fluid);
      }
   }

   @Override
   public boolean shouldBlockExplode(final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState state, final float power) {
      return this.explodesBlocks;
   }

   @Override
   public boolean shouldDamageEntity(final Explosion explosion, final Entity entity) {
      return this.damagesEntities;
   }

   @Override
   public float getKnockbackMultiplier(final Entity entity) {
      boolean creativeFlying = entity instanceof Player player && player.getAbilities().flying;
      return creativeFlying ? 0.0F : this.knockbackMultiplier.orElseGet(() -> super.getKnockbackMultiplier(entity));
   }
}
