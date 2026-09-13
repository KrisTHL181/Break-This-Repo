package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableLong;

public class TryFindWater {
   public static BehaviorControl<PathfinderMob> create(final int range, final float speedModifier) {
      MutableLong nextOkStartTime = new MutableLong(0L);
      return BehaviorBuilder.create(
         i -> i.group(i.absent(MemoryModuleType.ATTACK_TARGET), i.absent(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET))
            .apply(i, (attackTarget, walkTarget, lookTarget) -> (level, body, timestamp) -> {
               if (level.getFluidState(body.blockPosition()).is(FluidTags.WATER)) {
                  return false;
               }

               if (timestamp < nextOkStartTime.longValue()) {
                  nextOkStartTime.setValue(timestamp + 20L + 2L);
                  return true;
               }

               BlockPos bestPos = null;
               BlockPos bestAlternatePos = null;
               BlockPos bodyBlockPos = body.blockPosition();

               for (BlockPos pos : BlockPos.withinManhattan(bodyBlockPos, range, range, range)) {
                  if (pos.getX() != bodyBlockPos.getX() || pos.getZ() != bodyBlockPos.getZ()) {
                     BlockState aboveState = body.level().getBlockState(pos.above());
                     BlockState state = body.level().getBlockState(pos);
                     if (state.is(Blocks.WATER)) {
                        if (aboveState.isAir()) {
                           bestPos = pos.immutable();
                           break;
                        }

                        if (bestAlternatePos == null && !pos.closerToCenterThan(body.position(), 1.5)) {
                           bestAlternatePos = pos.immutable();
                        }
                     }
                  }
               }

               if (bestPos == null) {
                  bestPos = bestAlternatePos;
               }

               if (bestPos != null) {
                  lookTarget.set(new BlockPosTracker(bestPos));
                  walkTarget.set(new WalkTarget(new BlockPosTracker(bestPos), speedModifier, 0));
               }

               nextOkStartTime.setValue(timestamp + 40L);
               return true;
            })
      );
   }
}
