package net.minecraft.world.entity.monster.breeze;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BreezeUtil {
   private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 50.0;

   public static Vec3 randomPointBehindTarget(final LivingEntity enemy, final RandomSource random) {
      int spreadDegrees = 90;
      float viewAngle = enemy.yHeadRot + 180.0F + (float)random.nextGaussian() * 90.0F / 2.0F;
      float r = Mth.lerp(random.nextFloat(), 4.0F, 8.0F);
      Vec3 direction = Vec3.directionFromRotation(0.0F, viewAngle).scale(r);
      return enemy.position().add(direction);
   }

   public static boolean hasLineOfSight(final Breeze breeze, final Vec3 target) {
      Vec3 from = new Vec3(breeze.getX(), breeze.getY(), breeze.getZ());
      return target.distanceTo(from) > getMaxLineOfSightTestRange(breeze)
         ? false
         : breeze.level().clip(new ClipContext(from, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, breeze)).getType() == HitResult.Type.MISS;
   }

   private static double getMaxLineOfSightTestRange(final Breeze breeze) {
      return Math.max(50.0, breeze.getAttributeValue(Attributes.FOLLOW_RANGE));
   }
}
