package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class LongJumpUtil {
   public static Optional<Vec3> calculateJumpVectorForAngle(
      final Mob body, final Vec3 targetPos, final float maxJumpVelocity, final int angle, final boolean checkCollision
   ) {
      Vec3 mobPos = body.position();
      Vec3 directionVectorPlane = new Vec3(targetPos.x - mobPos.x, 0.0, targetPos.z - mobPos.z).normalize().scale(0.5);
      Vec3 targetPosition = targetPos.subtract(directionVectorPlane);
      Vec3 directionVector = targetPosition.subtract(mobPos);
      float angrad = angle * (float) Math.PI / 180.0F;
      double xzAng = Math.atan2(directionVector.z, directionVector.x);
      double r2 = directionVector.subtract(0.0, directionVector.y, 0.0).lengthSqr();
      double r = Math.sqrt(r2);
      double y = directionVector.y;
      double g = body.getGravity();
      double sin2ang = Math.sin(2.0F * angrad);
      double cosangsqr = Math.pow(Math.cos(angrad), 2.0);
      double sinangrad = Math.sin(angrad);
      double cosangrad = Math.cos(angrad);
      double sinxzAng = Math.sin(xzAng);
      double cosxzAng = Math.cos(xzAng);
      double v0sqr = r2 * g / (r * sin2ang - 2.0 * y * cosangsqr);
      if (v0sqr < 0.0) {
         return Optional.empty();
      }

      double v0 = Math.sqrt(v0sqr);
      if (v0 > maxJumpVelocity) {
         return Optional.empty();
      }

      double v0r = v0 * cosangrad;
      double v0y = v0 * sinangrad;
      if (checkCollision) {
         int samples = Mth.ceil(r / v0r) * 2;
         double ri = 0.0;
         Vec3 previousPos = null;
         EntityDimensions mobDimensions = body.getDimensions(Pose.LONG_JUMPING);

         for (int i = 0; i < samples - 1; i++) {
            ri += r / samples;
            double yi = sinangrad / cosangrad * ri - Math.pow(ri, 2.0) * g / (2.0 * v0sqr * Math.pow(cosangrad, 2.0));
            double xi = ri * cosxzAng;
            double zi = ri * sinxzAng;
            Vec3 samplePos = new Vec3(mobPos.x + xi, mobPos.y + yi, mobPos.z + zi);
            if (previousPos != null && !isClearTransition(body, mobDimensions, previousPos, samplePos)) {
               return Optional.empty();
            }

            previousPos = samplePos;
         }
      }

      return Optional.of(new Vec3(v0r * cosxzAng, v0y, v0r * sinxzAng).scale(0.95F));
   }

   private static boolean isClearTransition(final Mob body, final EntityDimensions entityDimensions, final Vec3 position1, final Vec3 position2) {
      Vec3 direction = position2.subtract(position1);
      double minDimension = Math.min(entityDimensions.width(), entityDimensions.height());
      int checks = Mth.ceil(direction.length() / minDimension);
      Vec3 normalizedDirection = direction.normalize();
      Vec3 nextPointToCheck = position1;

      for (int i = 0; i < checks; i++) {
         nextPointToCheck = i == checks - 1 ? position2 : nextPointToCheck.add(normalizedDirection.scale(minDimension * 0.9F));
         if (!body.level().noCollision(body, entityDimensions.makeBoundingBox(nextPointToCheck))) {
            return false;
         }
      }

      return true;
   }
}
