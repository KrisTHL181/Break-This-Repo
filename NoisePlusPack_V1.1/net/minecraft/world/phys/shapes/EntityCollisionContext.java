package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public class EntityCollisionContext implements CollisionContext {
   private final boolean descending;
   private final double entityBottom;
   private final boolean placement;
   private final ItemStack heldItem;
   private final boolean alwaysCollideWithFluid;
   private final @Nullable Entity entity;

   protected EntityCollisionContext(boolean p_365888_, boolean p_396699_, double p_396474_, ItemStack p_395757_, boolean p_426169_, @Nullable Entity p_82872_) {
      this.descending = p_365888_;
      this.placement = p_396699_;
      this.entityBottom = p_396474_;
      this.heldItem = p_395757_;
      this.alwaysCollideWithFluid = p_426169_;
      this.entity = p_82872_;
   }

   @Deprecated
   protected EntityCollisionContext(Entity p_198920_, boolean p_198916_, boolean p_394820_) {
      this(
         p_198920_.isDescending(),
         p_394820_,
         p_198920_.getY(),
         p_198920_ instanceof LivingEntity livingentity ? livingentity.getMainHandItem() : ItemStack.EMPTY,
         p_198916_,
         p_198920_
      );
   }

   @Override
   public boolean isHoldingItem(Item p_82879_) {
      return this.heldItem.is(p_82879_);
   }

   @Override
   public boolean alwaysCollideWithFluid() {
      return this.alwaysCollideWithFluid;
   }

   @Override
   public boolean canStandOnFluid(FluidState p_205115_, FluidState p_205116_) {
      return this.entity instanceof LivingEntity livingentity
         ? livingentity.canStandOnFluid(p_205116_) && !p_205115_.getType().isSame(p_205116_.getType())
         : false;
   }

   @Override
   public VoxelShape getCollisionShape(BlockState p_367344_, CollisionGetter p_362064_, BlockPos p_364238_) {
      return p_367344_.getCollisionShape(p_362064_, p_364238_, this);
   }

   @Override
   public boolean isDescending() {
      return this.descending;
   }

   @Override
   public boolean isAbove(VoxelShape p_82886_, BlockPos p_82887_, boolean p_82888_) {
      return this.entityBottom > p_82887_.getY() + p_82886_.max(Direction.Axis.Y) - 1.0E-5F;
   }

   public @Nullable Entity getEntity() {
      return this.entity;
   }

   @Override
   public boolean isPlacement() {
      return this.placement;
   }

   protected static class Empty extends EntityCollisionContext {
      protected static final CollisionContext WITHOUT_FLUID_COLLISIONS = new EntityCollisionContext.Empty(false);
      protected static final CollisionContext WITH_FLUID_COLLISIONS = new EntityCollisionContext.Empty(true);

      public Empty(boolean p_430882_) {
         super(false, false, -Double.MAX_VALUE, ItemStack.EMPTY, p_430882_, null);
      }

      @Override
      public boolean isAbove(VoxelShape p_430920_, BlockPos p_425533_, boolean p_428784_) {
         return p_428784_;
      }
   }
}
