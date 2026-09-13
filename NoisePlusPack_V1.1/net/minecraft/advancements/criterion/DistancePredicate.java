package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public record DistancePredicate(
   MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z, MinMaxBounds.Doubles horizontal, MinMaxBounds.Doubles absolute
) {
   public static final Codec<DistancePredicate> CODEC = RecordCodecBuilder.create(
      p_452729_ -> p_452729_.group(
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::x),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::y),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::z),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("horizontal", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::horizontal),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("absolute", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::absolute)
         )
         .apply(p_452729_, DistancePredicate::new)
   );

   public static DistancePredicate horizontal(MinMaxBounds.Doubles p_452688_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, p_452688_, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate vertical(MinMaxBounds.Doubles p_456233_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, p_456233_, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate absolute(MinMaxBounds.Doubles p_451041_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, p_451041_);
   }

   public boolean matches(double p_458719_, double p_453759_, double p_454963_, double p_451940_, double p_451569_, double p_459338_) {
      float f = (float)(p_458719_ - p_451940_);
      float f1 = (float)(p_453759_ - p_451569_);
      float f2 = (float)(p_454963_ - p_459338_);
      if (this.x.matches(Mth.abs(f)) && this.y.matches(Mth.abs(f1)) && this.z.matches(Mth.abs(f2))) {
         return !this.horizontal.matchesSqr(f * f + f2 * f2) ? false : this.absolute.matchesSqr(f * f + f1 * f1 + f2 * f2);
      } else {
         return false;
      }
   }
}
