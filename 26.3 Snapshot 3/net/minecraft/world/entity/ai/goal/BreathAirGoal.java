package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class BreathAirGoal extends Goal {
   private final PathfinderMob mob;

   public BreathAirGoal(final PathfinderMob mob) {
      this.mob = mob;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      return this.mob.getAirSupply() < 140;
   }

   @Override
   public boolean canContinueToUse() {
      return this.canUse();
   }

   @Override
   public boolean isInterruptable() {
      return false;
   }

   @Override
   public void start() {
      this.mob.getNavigation().stop();
      this.findAirPosition();
   }

   private void findAirPosition() {
      Iterable<BlockPos> between = BlockPos.neighborColumn(this.mob.getBlockX(), this.mob.getBlockY(), this.mob.getBlockZ(), this.mob.getBlockY() + 8);
      BlockPos destinationPos = null;

      for (BlockPos pos : between) {
         if (this.givesAir(this.mob.level(), pos)) {
            destinationPos = pos;
            break;
         }
      }

      if (destinationPos == null) {
         destinationPos = BlockPos.containing(this.mob.getX(), this.mob.getY() + 8.0, this.mob.getZ());
      }

      this.mob.getNavigation().moveTo(destinationPos.getX(), destinationPos.getY() + 1, destinationPos.getZ(), 1.0);
   }

   @Override
   public void tick() {
      this.findAirPosition();
      this.mob.moveRelative(0.02F, new Vec3(this.mob.xxa, this.mob.yya, this.mob.zza));
      this.mob.move(MoverType.SELF, this.mob.getDeltaMovement());
   }

   private boolean givesAir(final LevelReader level, final BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return (level.getFluidState(pos).isEmpty() || state.is(Blocks.BUBBLE_COLUMN)) && state.isPathfindable(PathComputationType.LAND);
   }
}
