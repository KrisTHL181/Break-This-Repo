package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public abstract class DoorInteractGoal extends Goal {
   protected Mob mob;
   protected BlockPos doorPos = BlockPos.ZERO;
   protected boolean hasDoor;
   private boolean passed;
   private float doorOpenDirX;
   private float doorOpenDirZ;

   public DoorInteractGoal(final Mob mob) {
      this.mob = mob;
      if (!GoalUtils.hasGroundPathNavigation(mob)) {
         throw new IllegalArgumentException("Unsupported mob type for DoorInteractGoal");
      }
   }

   protected boolean isOpen() {
      if (!this.hasDoor) {
         return false;
      } else {
         BlockState blockState = this.mob.level().getBlockState(this.doorPos);
         if (!(blockState.getBlock() instanceof DoorBlock)) {
            this.hasDoor = false;
            return false;
         } else {
            return blockState.getValue(DoorBlock.OPEN);
         }
      }
   }

   protected void setOpen(final boolean open) {
      if (this.hasDoor) {
         BlockState blockState = this.mob.level().getBlockState(this.doorPos);
         if (blockState.getBlock() instanceof DoorBlock) {
            ((DoorBlock)blockState.getBlock()).setOpen(this.mob, this.mob.level(), blockState, this.doorPos, open);
         }
      }
   }

   @Override
   public boolean canUse() {
      if (!GoalUtils.hasGroundPathNavigation(this.mob)) {
         return false;
      }

      if (!this.mob.horizontalCollision) {
         return false;
      }

      Path path = this.mob.getNavigation().getPath();
      if (path != null && !path.isDone()) {
         for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); i++) {
            Node node = path.getNode(i);
            this.doorPos = new BlockPos(node.x, node.y + 1, node.z);
            if (!(this.mob.distanceToSqr(this.doorPos.getX(), this.mob.getY(), this.doorPos.getZ()) > 2.25)) {
               this.hasDoor = DoorBlock.isWoodenDoor(this.mob.level(), this.doorPos);
               if (this.hasDoor) {
                  return true;
               }
            }
         }

         this.doorPos = this.mob.blockPosition().above();
         this.hasDoor = DoorBlock.isWoodenDoor(this.mob.level(), this.doorPos);
         return this.hasDoor;
      } else {
         return false;
      }
   }

   @Override
   public boolean canContinueToUse() {
      return !this.passed;
   }

   @Override
   public void start() {
      this.passed = false;
      this.doorOpenDirX = (float)(this.doorPos.getX() + 0.5 - this.mob.getX());
      this.doorOpenDirZ = (float)(this.doorPos.getZ() + 0.5 - this.mob.getZ());
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      float newDoorDirX = (float)(this.doorPos.getX() + 0.5 - this.mob.getX());
      float newDoorDirZ = (float)(this.doorPos.getZ() + 0.5 - this.mob.getZ());
      float dot = this.doorOpenDirX * newDoorDirX + this.doorOpenDirZ * newDoorDirZ;
      if (dot < 0.0F) {
         this.passed = true;
      }
   }
}
