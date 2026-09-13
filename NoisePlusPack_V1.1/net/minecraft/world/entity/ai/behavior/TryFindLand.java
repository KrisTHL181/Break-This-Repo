package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.apache.commons.lang3.mutable.MutableLong;

public class TryFindLand {
   private static final int COOLDOWN_TICKS = 60;

   public static BehaviorControl<PathfinderMob> create(int p_259889_, float p_259302_) {
      MutableLong mutablelong = new MutableLong(0L);
      return BehaviorBuilder.create(
         p_259851_ -> p_259851_.group(
               p_259851_.absent(MemoryModuleType.ATTACK_TARGET),
               p_259851_.absent(MemoryModuleType.WALK_TARGET),
               p_259851_.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(
               p_259851_,
               (p_259686_, p_259882_, p_259123_) -> (p_260032_, p_260019_, p_259854_) -> {
                  if (!p_260032_.getFluidState(p_260019_.blockPosition()).is(FluidTags.WATER)) {
                     return false;
                  }

                  if (p_259854_ < mutablelong.longValue()) {
                     mutablelong.setValue(p_259854_ + 60L);
                     return true;
                  }

                  BlockPos blockpos = p_260019_.blockPosition();
                  BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
                  CollisionContext collisioncontext = CollisionContext.of(p_260019_);

                  for (BlockPos blockpos1 : BlockPos.withinManhattan(blockpos, p_259889_, p_259889_, p_259889_)) {
                     if (blockpos1.getX() != blockpos.getX() || blockpos1.getZ() != blockpos.getZ()) {
                        BlockState blockstate = p_260032_.getBlockState(blockpos1);
                        BlockState blockstate1 = p_260032_.getBlockState(blockpos$mutableblockpos.setWithOffset(blockpos1, Direction.DOWN));
                        if (!blockstate.is(Blocks.WATER)
                           && p_260032_.getFluidState(blockpos1).isEmpty()
                           && blockstate.getCollisionShape(p_260032_, blockpos1, collisioncontext).isEmpty()
                           && blockstate1.isFaceSturdy(p_260032_, blockpos$mutableblockpos, Direction.UP)) {
                           BlockPos blockpos2 = blockpos1.immutable();
                           p_259123_.set(new BlockPosTracker(blockpos2));
                           p_259882_.set(new WalkTarget(new BlockPosTracker(blockpos2), p_259302_, 1));
                           break;
                        }
                     }
                  }

                  mutablelong.setValue(p_259854_ + 60L);
                  return true;
               }
            )
      );
   }
}
