package net.minecraft.advancements.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.food.FoodData;

public record FoodPredicate(MinMaxBounds.Ints level, MinMaxBounds.Doubles saturation) {
   public static final FoodPredicate ANY = new FoodPredicate(MinMaxBounds.Ints.ANY, MinMaxBounds.Doubles.ANY);
   public static final Codec<FoodPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(FoodPredicate::level),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("saturation", MinMaxBounds.Doubles.ANY).forGetter(FoodPredicate::saturation)
         )
         .apply(i, FoodPredicate::new)
   );

   public boolean matches(final FoodData food) {
      return !this.level.matches(food.getFoodLevel()) ? false : this.saturation.matches(food.getSaturationLevel());
   }

   public static class Builder {
      private MinMaxBounds.Ints level = MinMaxBounds.Ints.ANY;
      private MinMaxBounds.Doubles saturation = MinMaxBounds.Doubles.ANY;

      public FoodPredicate.Builder withLevel(final MinMaxBounds.Ints level) {
         this.level = level;
         return this;
      }

      public FoodPredicate.Builder withSaturation(final MinMaxBounds.Doubles saturation) {
         this.saturation = saturation;
         return this;
      }

      public static FoodPredicate.Builder food() {
         return new FoodPredicate.Builder();
      }

      public FoodPredicate build() {
         return new FoodPredicate(this.level, this.saturation);
      }
   }
}
