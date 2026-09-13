package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public record MovementPredicate(
   MinMaxBounds.Doubles x,
   MinMaxBounds.Doubles y,
   MinMaxBounds.Doubles z,
   MinMaxBounds.Doubles speed,
   MinMaxBounds.Doubles horizontalSpeed,
   MinMaxBounds.Doubles verticalSpeed,
   MinMaxBounds.Doubles fallDistance
) {
   public static final Codec<MovementPredicate> CODEC = RecordCodecBuilder.create(
      p_455817_ -> p_455817_.group(
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::x),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::y),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::z),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("speed", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::speed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("horizontal_speed", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::horizontalSpeed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("vertical_speed", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::verticalSpeed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("fall_distance", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::fallDistance)
         )
         .apply(p_455817_, MovementPredicate::new)
   );

   public static MovementPredicate speed(MinMaxBounds.Doubles p_451385_) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         p_451385_,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY
      );
   }

   public static MovementPredicate horizontalSpeed(MinMaxBounds.Doubles p_451546_) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         p_451546_,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY
      );
   }

   public static MovementPredicate verticalSpeed(MinMaxBounds.Doubles p_459001_) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         p_459001_,
         MinMaxBounds.Doubles.ANY
      );
   }

   public static MovementPredicate fallDistance(MinMaxBounds.Doubles p_451472_) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         p_451472_
      );
   }

   public boolean matches(double p_454324_, double p_452238_, double p_459668_, double p_454018_) {
      if (this.x.matches(p_454324_) && this.y.matches(p_452238_) && this.z.matches(p_459668_)) {
         double d0 = Mth.lengthSquared(p_454324_, p_452238_, p_459668_);
         if (!this.speed.matchesSqr(d0)) {
            return false;
         }

         double d1 = Mth.lengthSquared(p_454324_, p_459668_);
         if (!this.horizontalSpeed.matchesSqr(d1)) {
            return false;
         }

         double d2 = Math.abs(p_452238_);
         return !this.verticalSpeed.matches(d2) ? false : this.fallDistance.matches(p_454018_);
      } else {
         return false;
      }
   }
}
