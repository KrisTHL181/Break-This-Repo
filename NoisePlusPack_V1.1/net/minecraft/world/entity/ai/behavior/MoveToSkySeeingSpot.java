package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MoveToSkySeeingSpot {
   public static OneShot<LivingEntity> create(float p_259860_) {
      return BehaviorBuilder.create(
         p_258543_ -> p_258543_.group(p_258543_.absent(MemoryModuleType.WALK_TARGET)).apply(p_258543_, p_258545_ -> (p_449518_, p_449519_, p_449520_) -> {
            if (p_449518_.canSeeSky(p_449519_.blockPosition())) {
               return false;
            }

            Optional<Vec3> optional = Optional.ofNullable(getOutdoorPosition(p_449518_, p_449519_));
            optional.ifPresent(p_258548_ -> p_258545_.set(new WalkTarget(p_258548_, p_259860_, 0)));
            return true;
         })
      );
   }

   private static @Nullable Vec3 getOutdoorPosition(ServerLevel p_23565_, LivingEntity p_23566_) {
      RandomSource randomsource = p_23566_.getRandom();
      BlockPos blockpos = p_23566_.blockPosition();

      for (int i = 0; i < 10; i++) {
         BlockPos blockpos1 = blockpos.offset(randomsource.nextInt(20) - 10, randomsource.nextInt(6) - 3, randomsource.nextInt(20) - 10);
         if (hasNoBlocksAbove(p_23565_, p_23566_, blockpos1)) {
            return Vec3.atBottomCenterOf(blockpos1);
         }
      }

      return null;
   }

   public static boolean hasNoBlocksAbove(ServerLevel p_23559_, LivingEntity p_23560_, BlockPos p_23561_) {
      return p_23559_.canSeeSky(p_23561_) && p_23559_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p_23561_).getY() <= p_23560_.getY();
   }
}
