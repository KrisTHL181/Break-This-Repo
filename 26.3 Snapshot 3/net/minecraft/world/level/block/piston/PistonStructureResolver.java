package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class PistonStructureResolver {
   public static final int MAX_PUSH_DEPTH = 12;
   private final Level level;
   private final BlockPos pistonPos;
   private final boolean extending;
   private final BlockPos startPos;
   private final Direction pushDirection;
   private final List<BlockPos> toPush = Lists.newArrayList();
   private final List<BlockPos> toDestroy = Lists.newArrayList();
   private final Direction pistonDirection;

   public PistonStructureResolver(final Level level, final BlockPos pistonPos, final Direction direction, final boolean extending) {
      this.level = level;
      this.pistonPos = pistonPos;
      this.pistonDirection = direction;
      this.extending = extending;
      if (extending) {
         this.pushDirection = direction;
         this.startPos = pistonPos.relative(direction);
      } else {
         this.pushDirection = direction.getOpposite();
         this.startPos = pistonPos.relative(direction, 2);
      }
   }

   public boolean resolve() {
      this.toPush.clear();
      this.toDestroy.clear();
      BlockState nextState = this.level.getBlockState(this.startPos);
      if (!PistonBaseBlock.isPushable(nextState, this.level, this.startPos, this.pushDirection, false, this.pistonDirection)) {
         if (this.extending && nextState.getPistonPushReaction() == PushReaction.DESTROY) {
            this.toDestroy.add(this.startPos);
            return true;
         } else {
            return false;
         }
      } else {
         if (!this.addBlockLine(this.startPos, this.pushDirection)) {
            return false;
         }

         for (int i = 0; i < this.toPush.size(); i++) {
            BlockPos pos = this.toPush.get(i);
            if (isSticky(this.level.getBlockState(pos)) && !this.addBranchingBlocks(pos)) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean isSticky(final BlockState state) {
      return state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK);
   }

   private static boolean canStickToEachOther(final BlockState state1, final BlockState state2) {
      if (state1.is(Blocks.HONEY_BLOCK) && state2.is(Blocks.SLIME_BLOCK)) {
         return false;
      } else {
         return state1.is(Blocks.SLIME_BLOCK) && state2.is(Blocks.HONEY_BLOCK) ? false : isSticky(state1) || isSticky(state2);
      }
   }

   private boolean addBlockLine(final BlockPos start, final Direction direction) {
      BlockState nextState = this.level.getBlockState(start);
      if (nextState.isAir()) {
         return true;
      }

      if (!PistonBaseBlock.isPushable(nextState, this.level, start, this.pushDirection, false, direction)) {
         return true;
      }

      if (start.equals(this.pistonPos)) {
         return true;
      }

      if (this.toPush.contains(start)) {
         return true;
      }

      int blockCount = 1;
      if (blockCount + this.toPush.size() > 12) {
         return false;
      }

      while (isSticky(nextState)) {
         BlockPos pos = start.relative(this.pushDirection.getOpposite(), blockCount);
         BlockState previousState = nextState;
         nextState = this.level.getBlockState(pos);
         if (nextState.isAir()
            || !canStickToEachOther(previousState, nextState)
            || !PistonBaseBlock.isPushable(nextState, this.level, pos, this.pushDirection, false, this.pushDirection.getOpposite())
            || pos.equals(this.pistonPos)) {
            break;
         }

         if (++blockCount + this.toPush.size() > 12) {
            return false;
         }
      }

      int blocksAdded = 0;

      for (int i = blockCount - 1; i >= 0; i--) {
         this.toPush.add(start.relative(this.pushDirection.getOpposite(), i));
         blocksAdded++;
      }

      int i = 1;

      while (true) {
         BlockPos pos = start.relative(this.pushDirection, i);
         int collisionPos = this.toPush.indexOf(pos);
         if (collisionPos > -1) {
            this.reorderListAtCollision(blocksAdded, collisionPos);

            for (int j = 0; j <= collisionPos + blocksAdded; j++) {
               BlockPos blockPos = this.toPush.get(j);
               if (isSticky(this.level.getBlockState(blockPos)) && !this.addBranchingBlocks(blockPos)) {
                  return false;
               }
            }

            return true;
         }

         nextState = this.level.getBlockState(pos);
         if (nextState.isAir()) {
            return true;
         }

         if (!PistonBaseBlock.isPushable(nextState, this.level, pos, this.pushDirection, true, this.pushDirection) || pos.equals(this.pistonPos)) {
            return false;
         }

         if (nextState.getPistonPushReaction() == PushReaction.DESTROY) {
            this.toDestroy.add(pos);
            return true;
         }

         if (this.toPush.size() >= 12) {
            return false;
         }

         this.toPush.add(pos);
         blocksAdded++;
         i++;
      }
   }

   private void reorderListAtCollision(final int blocksAdded, final int collisionPos) {
      List<BlockPos> head = Lists.newArrayList();
      List<BlockPos> lastLineAdded = Lists.newArrayList();
      List<BlockPos> collisionToLine = Lists.newArrayList();
      head.addAll(this.toPush.subList(0, collisionPos));
      lastLineAdded.addAll(this.toPush.subList(this.toPush.size() - blocksAdded, this.toPush.size()));
      collisionToLine.addAll(this.toPush.subList(collisionPos, this.toPush.size() - blocksAdded));
      this.toPush.clear();
      this.toPush.addAll(head);
      this.toPush.addAll(lastLineAdded);
      this.toPush.addAll(collisionToLine);
   }

   private boolean addBranchingBlocks(final BlockPos fromPos) {
      BlockState fromState = this.level.getBlockState(fromPos);

      for (Direction direction : Direction.values()) {
         if (direction.getAxis() != this.pushDirection.getAxis()) {
            BlockPos neighbourPos = fromPos.relative(direction);
            BlockState neighbourState = this.level.getBlockState(neighbourPos);
            if (canStickToEachOther(neighbourState, fromState) && !this.addBlockLine(neighbourPos, direction)) {
               return false;
            }
         }
      }

      return true;
   }

   public Direction getPushDirection() {
      return this.pushDirection;
   }

   public List<BlockPos> getToPush() {
      return this.toPush;
   }

   public List<BlockPos> getToDestroy() {
      return this.toDestroy;
   }
}
