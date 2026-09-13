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
   public static OneShot<LivingEntity> create(final float speedModifier) {
      return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET)).apply(i, walkTarget -> (level, body, timestamp) -> {
         if (level.canSeeSky(body.blockPosition())) {
            return false;
         }

         Optional<Vec3> landPos = Optional.ofNullable(getOutdoorPosition(level, body));
         landPos.ifPresent(pos -> walkTarget.set(new WalkTarget(pos, speedModifier, 0)));
         return true;
      }));
   }

   private static @Nullable Vec3 getOutdoorPosition(final ServerLevel level, final LivingEntity body) {
      RandomSource random = body.getRandom();
      BlockPos pos = body.blockPosition();

      for (int i = 0; i < 10; i++) {
         BlockPos randomPos = pos.offset(random.nextInt(20) - 10, random.nextInt(6) - 3, random.nextInt(20) - 10);
         if (hasNoBlocksAbove(level, body, randomPos)) {
            return Vec3.atBottomCenterOf(randomPos);
         }
      }

      return null;
   }

   public static boolean hasNoBlocksAbove(final ServerLevel level, final LivingEntity body, final BlockPos target) {
      return level.canSeeSky(target) && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target).getY() <= body.getY();
   }
}
