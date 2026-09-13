package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record MovementPredicate(
   MinMaxBounds.Doubles x,
   MinMaxBounds.Doubles y,
   MinMaxBounds.Doubles z,
   MinMaxBounds.Doubles speed,
   MinMaxBounds.Doubles horizontalSpeed,
   MinMaxBounds.Doubles verticalSpeed,
   MinMaxBounds.Doubles fallDistance
) implements EntitySubPredicate {
   public static final Codec<MovementPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::x),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::y),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::z),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("speed", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::speed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("horizontal_speed", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::horizontalSpeed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("vertical_speed", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::verticalSpeed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("fall_distance", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::fallDistance)
         )
         .apply(i, MovementPredicate::new)
   );

   public static MovementPredicate speed(final MinMaxBounds.Doubles bounds) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         bounds,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY
      );
   }

   public static MovementPredicate horizontalSpeed(final MinMaxBounds.Doubles bounds) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         bounds,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY
      );
   }

   public static MovementPredicate verticalSpeed(final MinMaxBounds.Doubles bounds) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         bounds,
         MinMaxBounds.Doubles.ANY
      );
   }

   public static MovementPredicate fallDistance(final MinMaxBounds.Doubles bounds) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         bounds
      );
   }

   public boolean matches(final double x, final double y, final double z, final double fallDistance) {
      if (this.x.matches(x) && this.y.matches(y) && this.z.matches(z)) {
         double speedSqr = Mth.lengthSquared(x, y, z);
         if (!this.speed.matchesSqr(speedSqr)) {
            return false;
         }

         double horizontalSpeedSqr = Mth.lengthSquared(x, z);
         if (!this.horizontalSpeed.matchesSqr(horizontalSpeedSqr)) {
            return false;
         }

         double verticalSpeed = Math.abs(y);
         return !this.verticalSpeed.matches(verticalSpeed) ? false : this.fallDistance.matches(fallDistance);
      } else {
         return false;
      }
   }

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      Vec3 velocity = entity.getKnownMovement().scale(20.0);
      return this.matches(velocity.x, velocity.y, velocity.z, entity.fallDistance);
   }
}
