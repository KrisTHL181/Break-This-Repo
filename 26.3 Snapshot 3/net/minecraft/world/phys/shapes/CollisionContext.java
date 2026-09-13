package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public interface CollisionContext {
   static CollisionContext empty() {
      return EntityCollisionContext.Empty.WITHOUT_FLUID_COLLISIONS;
   }

   static CollisionContext emptyWithFluidCollisions() {
      return EntityCollisionContext.Empty.WITH_FLUID_COLLISIONS;
   }

   static CollisionContext of(final Entity entity) {
      return switch (entity) {
         case AbstractMinecart minecart -> AbstractMinecart.useExperimentalMovement(minecart.level())
            ? new MinecartCollisionContext(minecart, false)
            : new EntityCollisionContext(entity, false, false);
         default -> new EntityCollisionContext(entity, false, false);
      };
   }

   static CollisionContext of(final Entity entity, final boolean alwaysCollideWithFluid) {
      return new EntityCollisionContext(entity, alwaysCollideWithFluid, false);
   }

   static CollisionContext positionContext(final double y) {
      return new PositionCollisionContext(y);
   }

   static CollisionContext placementContext(final @Nullable Player player) {
      return new EntityCollisionContext(
         player != null ? player.isDescending() : false,
         true,
         player != null ? player.getY() : -Double.MAX_VALUE,
         player != null ? player.getMainHandItem() : ItemStack.EMPTY,
         false,
         player
      );
   }

   static CollisionContext withPosition(final @Nullable Entity entity, final double position) {
      return new EntityCollisionContext(
         entity != null ? entity.isDescending() : false,
         true,
         entity != null ? position : -Double.MAX_VALUE,
         entity instanceof LivingEntity livingEntity ? livingEntity.getMainHandItem() : ItemStack.EMPTY,
         false,
         entity
      );
   }

   boolean isDescending();

   boolean isAbove(final VoxelShape shape, final BlockPos pos, final boolean defaultValue);

   boolean isHoldingItem(final Item item);

   boolean alwaysCollideWithFluid();

   boolean canStandOnFluid(final FluidState fluidStateAbove, final FluidState fluid);

   VoxelShape getCollisionShape(BlockState state, CollisionGetter collisionGetter, BlockPos pos);

   default boolean isPlacement() {
      return false;
   }
}
