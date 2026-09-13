package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.apache.commons.lang3.mutable.MutableLong;

public class TryFindLandNearWater {
   public static BehaviorControl<PathfinderMob> create(int p_259739_, float p_259118_) {
      MutableLong mutablelong = new MutableLong(0L);
      return BehaviorBuilder.create(
         p_260348_ -> p_260348_.group(
               p_260348_.absent(MemoryModuleType.ATTACK_TARGET),
               p_260348_.absent(MemoryModuleType.WALK_TARGET),
               p_260348_.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(
               p_260348_,
               (p_259029_, p_259100_, p_259367_) -> (p_259876_, p_259531_, p_259771_) -> {
                  if (p_259876_.getFluidState(p_259531_.blockPosition()).is(FluidTags.WATER)) {
                     return false;
                  }

                  if (p_259771_ < mutablelong.longValue()) {
                     mutablelong.setValue(p_259771_ + 40L);
                     return true;
                  }

                  CollisionContext collisioncontext = CollisionContext.of(p_259531_);
                  BlockPos blockpos = p_259531_.blockPosition();
                  BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                  label42:
                  for (BlockPos blockpos1 : BlockPos.withinManhattan(blockpos, p_259739_, p_259739_, p_259739_)) {
                     if ((blockpos1.getX() != blockpos.getX() || blockpos1.getZ() != blockpos.getZ())
                        && p_259876_.getBlockState(blockpos1).getCollisionShape(p_259876_, blockpos1, collisioncontext).isEmpty()
                        && !p_259876_.getBlockState(blockpos$mutableblockpos.setWithOffset(blockpos1, Direction.DOWN))
                           .getCollisionShape(p_259876_, blockpos1, collisioncontext)
                           .isEmpty()) {
                        for (Direction direction : Direction.Plane.HORIZONTAL) {
                           blockpos$mutableblockpos.setWithOffset(blockpos1, direction);
                           if (p_259876_.getBlockState(blockpos$mutableblockpos).isAir()
                              && p_259876_.getBlockState(blockpos$mutableblockpos.move(Direction.DOWN)).is(Blocks.WATER)) {
                              p_259367_.set(new BlockPosTracker(blockpos1));
                              p_259100_.set(new WalkTarget(new BlockPosTracker(blockpos1), p_259118_, 0));
                              break label42;
                           }
                        }
                     }
                  }

                  mutablelong.setValue(p_259771_ + 40L);
                  return true;
               }
            )
      );
   }
}
