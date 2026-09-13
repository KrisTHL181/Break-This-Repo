package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jspecify.annotations.Nullable;

public class MinecartCollisionContext extends EntityCollisionContext {
   private @Nullable BlockPos ingoreBelow;
   private @Nullable BlockPos slopeIgnore;

   protected MinecartCollisionContext(AbstractMinecart p_456109_, boolean p_363030_) {
      super(p_456109_, p_363030_, false);
      this.setupContext(p_456109_);
   }

   private void setupContext(AbstractMinecart p_454602_) {
      BlockPos blockpos = p_454602_.getCurrentBlockPosOrRailBelow();
      BlockState blockstate = p_454602_.level().getBlockState(blockpos);
      boolean flag = BaseRailBlock.isRail(blockstate);
      if (flag) {
         this.ingoreBelow = blockpos.below();
         RailShape railshape = blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty());
         if (railshape.isSlope()) {
            this.slopeIgnore = switch (railshape) {
               case ASCENDING_EAST -> blockpos.east();
               case ASCENDING_WEST -> blockpos.west();
               case ASCENDING_NORTH -> blockpos.north();
               case ASCENDING_SOUTH -> blockpos.south();
               default -> null;
            };
         }
      }
   }

   @Override
   public VoxelShape getCollisionShape(BlockState p_361633_, CollisionGetter p_368990_, BlockPos p_365642_) {
      return !p_365642_.equals(this.ingoreBelow) && !p_365642_.equals(this.slopeIgnore)
         ? super.getCollisionShape(p_361633_, p_368990_, p_365642_)
         : Shapes.empty();
   }
}
