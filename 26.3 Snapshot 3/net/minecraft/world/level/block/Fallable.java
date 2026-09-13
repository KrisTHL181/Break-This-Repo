package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface Fallable {
   default void onLand(final Level level, final BlockPos pos, final BlockState state, final BlockState replacedBlock, final FallingBlockEntity entity) {
   }

   default void onBrokenAfterFall(final Level level, final BlockPos pos, final FallingBlockEntity entity) {
   }

   default DamageSource getFallDamageSource(final Entity entity) {
      return entity.damageSources().fallingBlock(entity);
   }
}
