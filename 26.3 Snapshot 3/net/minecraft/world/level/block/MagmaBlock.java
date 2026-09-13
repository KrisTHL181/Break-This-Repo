package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MagmaBlock extends Block {
   public MagmaBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public void stepOn(final Level level, final BlockPos pos, final BlockState onState, final Entity entity) {
      if (!entity.isSteppingCarefully() && entity instanceof LivingEntity) {
         entity.hurt(level.damageSources().hotFloor(), 1.0F);
      }

      super.stepOn(level, pos, onState, entity);
   }
}
