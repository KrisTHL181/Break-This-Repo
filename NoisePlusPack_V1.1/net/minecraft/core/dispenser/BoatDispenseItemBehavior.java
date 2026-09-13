package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public class BoatDispenseItemBehavior extends DefaultDispenseItemBehavior {
   private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
   private final EntityType<? extends AbstractBoat> type;

   public BoatDispenseItemBehavior(EntityType<? extends AbstractBoat> p_369323_) {
      this.type = p_369323_;
   }

   @Override
   public ItemStack execute(BlockSource p_123375_, ItemStack p_123376_) {
      Direction direction = p_123375_.state().getValue(DispenserBlock.FACING);
      ServerLevel serverlevel = p_123375_.level();
      Vec3 vec3 = p_123375_.center();
      double d0 = 0.5625 + this.type.getWidth() / 2.0;
      double d1 = vec3.x() + direction.getStepX() * d0;
      double d2 = vec3.y() + direction.getStepY() * 1.125F;
      double d3 = vec3.z() + direction.getStepZ() * d0;
      BlockPos blockpos = p_123375_.pos().relative(direction);
      double d4;
      if (serverlevel.getFluidState(blockpos).is(FluidTags.WATER)) {
         d4 = 1.0;
      } else {
         if (!serverlevel.getBlockState(blockpos).isAir() || !serverlevel.getFluidState(blockpos.below()).is(FluidTags.WATER)) {
            return this.defaultDispenseItemBehavior.dispense(p_123375_, p_123376_);
         }

         d4 = 0.0;
      }

      AbstractBoat abstractboat = this.type.create(serverlevel, EntitySpawnReason.DISPENSER);
      if (abstractboat != null) {
         abstractboat.setInitialPos(d1, d2 + d4, d3);
         EntityType.<AbstractBoat>createDefaultStackConfig(serverlevel, p_123376_, null).accept(abstractboat);
         abstractboat.setYRot(direction.toYRot());
         serverlevel.addFreshEntity(abstractboat);
         p_123376_.shrink(1);
      }

      return p_123376_;
   }

   @Override
   protected void playSound(BlockSource p_123373_) {
      p_123373_.level().levelEvent(1000, p_123373_.pos(), 0);
   }
}
