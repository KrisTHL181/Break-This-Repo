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

   public DoorInteractGoal(Mob p_25193_) {
      this.mob = p_25193_;
      if (!GoalUtils.hasGroundPathNavigation(p_25193_)) {
         throw new IllegalArgumentException("Unsupported mob type for DoorInteractGoal");
      }
   }

   protected boolean isOpen() {
      if (!this.hasDoor) {
         return false;
      } else {
         BlockState blockstate = this.mob.level().getBlockState(this.doorPos);
         if (!(blockstate.getBlock() instanceof DoorBlock)) {
            this.hasDoor = false;
            return false;
         } else {
            return blockstate.getValue(DoorBlock.OPEN);
         }
      }
   }

   protected void setOpen(boolean p_25196_) {
      if (this.hasDoor) {
         BlockState blockstate = this.mob.level().getBlockState(this.doorPos);
         if (blockstate.getBlock() instanceof DoorBlock) {
            ((DoorBlock)blockstate.getBlock()).setOpen(this.mob, this.mob.level(), blockstate, this.doorPos, p_25196_);
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
      float f = (float)(this.doorPos.getX() + 0.5 - this.mob.getX());
      float f1 = (float)(this.doorPos.getZ() + 0.5 - this.mob.getZ());
      float f2 = this.doorOpenDirX * f + this.doorOpenDirZ * f1;
      if (f2 < 0.0F) {
         this.passed = true;
      }
   }
}
