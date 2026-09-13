package net.minecraft.world.entity.ai.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RandomPos {
   private static final int RANDOM_POS_ATTEMPTS = 10;

   public static BlockPos generateRandomDirection(final RandomSource random, final int horizontalDist, final int verticalDist) {
      int xt = random.nextInt(2 * horizontalDist + 1) - horizontalDist;
      int yt = random.nextInt(2 * verticalDist + 1) - verticalDist;
      int zt = random.nextInt(2 * horizontalDist + 1) - horizontalDist;
      return new BlockPos(xt, yt, zt);
   }

   public static @Nullable BlockPos generateRandomDirectionWithinRadians(
      final RandomSource random,
      final double minHorizontalDist,
      final double maxHorizontalDist,
      final int verticalDist,
      final int flyingHeight,
      final double xDir,
      final double zDir,
      final double maxXzRadiansFromDir
   ) {
      double yRadiansCenter = Mth.atan2(zDir, xDir) - (float) (Math.PI / 2);
      double yRadians = yRadiansCenter + (2.0F * random.nextFloat() - 1.0F) * maxXzRadiansFromDir;
      double dist = Mth.lerp(Math.sqrt(random.nextDouble()), minHorizontalDist, maxHorizontalDist) * Mth.SQRT_OF_TWO;
      double xt = -dist * Math.sin(yRadians);
      double zt = dist * Math.cos(yRadians);
      if (!(Math.abs(xt) > maxHorizontalDist) && !(Math.abs(zt) > maxHorizontalDist)) {
         int yt = random.nextInt(2 * verticalDist + 1) - verticalDist + flyingHeight;
         return BlockPos.containing(xt, yt, zt);
      } else {
         return null;
      }
   }

   @VisibleForTesting
   public static BlockPos moveUpOutOfSolid(final BlockPos pos, final int maxY, final Predicate<BlockPos> solidityTester) {
      if (!solidityTester.test(pos)) {
         return pos;
      }

      BlockPos.MutableBlockPos onGroundPos = pos.mutable().move(Direction.UP);

      while (onGroundPos.getY() <= maxY && solidityTester.test(onGroundPos)) {
         onGroundPos.move(Direction.UP);
      }

      return onGroundPos.immutable();
   }

   @VisibleForTesting
   public static BlockPos moveUpToAboveSolid(final BlockPos pos, final int aboveSolidAmount, final int maxY, final Predicate<BlockPos> solidityTester) {
      if (aboveSolidAmount < 0) {
         throw new IllegalArgumentException("aboveSolidAmount was " + aboveSolidAmount + ", expected >= 0");
      }

      if (!solidityTester.test(pos)) {
         return pos;
      }

      BlockPos.MutableBlockPos mutablePos = pos.mutable().move(Direction.UP);

      while (mutablePos.getY() <= maxY && solidityTester.test(mutablePos)) {
         mutablePos.move(Direction.UP);
      }

      int firstNonSolidY = mutablePos.getY();

      while (mutablePos.getY() <= maxY && mutablePos.getY() - firstNonSolidY < aboveSolidAmount) {
         mutablePos.move(Direction.UP);
         if (solidityTester.test(mutablePos)) {
            mutablePos.move(Direction.DOWN);
            break;
         }
      }

      return mutablePos.immutable();
   }

   public static @Nullable Vec3 generateRandomPos(final PathfinderMob mob, final Supplier<@Nullable BlockPos> posSupplier) {
      return generateRandomPos(posSupplier, mob::getWalkTargetValue);
   }

   public static @Nullable Vec3 generateRandomPos(final Supplier<@Nullable BlockPos> posSupplier, final ToDoubleFunction<BlockPos> positionWeightFunction) {
      double bestWeight = Double.NEGATIVE_INFINITY;
      BlockPos bestPos = null;

      for (int i = 0; i < 10; i++) {
         BlockPos pos = posSupplier.get();
         if (pos != null) {
            double value = positionWeightFunction.applyAsDouble(pos);
            if (value > bestWeight) {
               bestWeight = value;
               bestPos = pos;
            }
         }
      }

      return bestPos != null ? Vec3.atBottomCenterOf(bestPos) : null;
   }

   public static BlockPos generateRandomPosTowardDirection(final PathfinderMob mob, final double xzDist, final RandomSource random, final BlockPos direction) {
      double xt = direction.getX();
      double zt = direction.getZ();
      if (mob.hasHome() && xzDist > 1.0) {
         BlockPos center = mob.getHomePosition();
         if (mob.getX() > center.getX()) {
            xt -= random.nextDouble() * xzDist / 2.0;
         } else {
            xt += random.nextDouble() * xzDist / 2.0;
         }

         if (mob.getZ() > center.getZ()) {
            zt -= random.nextDouble() * xzDist / 2.0;
         } else {
            zt += random.nextDouble() * xzDist / 2.0;
         }
      }

      return BlockPos.containing(xt + mob.getX(), direction.getY() + mob.getY(), zt + mob.getZ());
   }
}
