package net.minecraft.util;

import net.minecraft.core.Direction;

public class SegmentedAnglePrecision {
   private final int mask;
   private final int precision;
   private final float degreeToAngle;
   private final float angleToDegree;

   public SegmentedAnglePrecision(int p_265275_) {
      if (p_265275_ < 2) {
         throw new IllegalArgumentException("Precision cannot be less than 2 bits");
      }

      if (p_265275_ > 30) {
         throw new IllegalArgumentException("Precision cannot be greater than 30 bits");
      }

      int i = 1 << p_265275_;
      this.mask = i - 1;
      this.precision = p_265275_;
      this.degreeToAngle = i / 360.0F;
      this.angleToDegree = 360.0F / i;
   }

   public boolean isSameAxis(int p_265505_, int p_265708_) {
      int i = this.getMask() >> 1;
      return (p_265505_ & i) == (p_265708_ & i);
   }

   public int fromDirection(Direction p_265731_) {
      if (p_265731_.getAxis().isVertical()) {
         return 0;
      }

      int i = p_265731_.get2DDataValue();
      return i << this.precision - 2;
   }

   public int fromDegreesWithTurns(float p_265346_) {
      return Math.round(p_265346_ * this.degreeToAngle);
   }

   public int fromDegrees(float p_265688_) {
      return this.normalize(this.fromDegreesWithTurns(p_265688_));
   }

   public float toDegreesWithTurns(int p_265278_) {
      return p_265278_ * this.angleToDegree;
   }

   public float toDegrees(int p_265623_) {
      float f = this.toDegreesWithTurns(this.normalize(p_265623_));
      return f >= 180.0F ? f - 360.0F : f;
   }

   public int normalize(int p_265542_) {
      return p_265542_ & this.mask;
   }

   public int getMask() {
      return this.mask;
   }
}
