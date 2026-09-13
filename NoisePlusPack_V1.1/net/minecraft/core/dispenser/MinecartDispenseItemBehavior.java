package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public class MinecartDispenseItemBehavior extends DefaultDispenseItemBehavior {
   private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
   private final EntityType<? extends AbstractMinecart> entityType;

   public MinecartDispenseItemBehavior(EntityType<? extends AbstractMinecart> p_367202_) {
      this.entityType = p_367202_;
   }

   @Override
   public ItemStack execute(BlockSource p_366036_, ItemStack p_368867_) {
      Direction direction = p_366036_.state().getValue(DispenserBlock.FACING);
      ServerLevel serverlevel = p_366036_.level();
      Vec3 vec3 = p_366036_.center();
      double d0 = vec3.x() + direction.getStepX() * 1.125;
      double d1 = Math.floor(vec3.y()) + direction.getStepY();
      double d2 = vec3.z() + direction.getStepZ() * 1.125;
      BlockPos blockpos = p_366036_.pos().relative(direction);
      BlockState blockstate = serverlevel.getBlockState(blockpos);
      double d3;
      if (blockstate.is(BlockTags.RAILS)) {
         if (getRailShape(blockstate).isSlope()) {
            d3 = 0.6;
         } else {
            d3 = 0.1;
         }
      } else {
         if (!blockstate.isAir()) {
            return this.defaultDispenseItemBehavior.dispense(p_366036_, p_368867_);
         }

         BlockState blockstate1 = serverlevel.getBlockState(blockpos.below());
         if (!blockstate1.is(BlockTags.RAILS)) {
            return this.defaultDispenseItemBehavior.dispense(p_366036_, p_368867_);
         }

         if (direction != Direction.DOWN && getRailShape(blockstate1).isSlope()) {
            d3 = -0.4;
         } else {
            d3 = -0.9;
         }
      }

      Vec3 vec31 = new Vec3(d0, d1 + d3, d2);
      AbstractMinecart abstractminecart = AbstractMinecart.createMinecart(
         serverlevel, vec31.x, vec31.y, vec31.z, this.entityType, EntitySpawnReason.DISPENSER, p_368867_, null
      );
      if (abstractminecart != null) {
         serverlevel.addFreshEntity(abstractminecart);
         p_368867_.shrink(1);
      }

      return p_368867_;
   }

   private static RailShape getRailShape(BlockState p_364080_) {
      return p_364080_.getBlock() instanceof BaseRailBlock baserailblock ? p_364080_.getValue(baserailblock.getShapeProperty()) : RailShape.NORTH_SOUTH;
   }

   @Override
   protected void playSound(BlockSource p_362755_) {
      p_362755_.level().levelEvent(1000, p_362755_.pos(), 0);
   }
}
