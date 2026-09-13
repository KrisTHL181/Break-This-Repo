package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;

public record DamagePredicate(MinMaxBounds.Ints durability, MinMaxBounds.Ints damage) implements DataComponentPredicate {
   public static final Codec<DamagePredicate> CODEC = RecordCodecBuilder.create(
      p_448614_ -> p_448614_.group(
            MinMaxBounds.Ints.CODEC.optionalFieldOf("durability", MinMaxBounds.Ints.ANY).forGetter(DamagePredicate::durability),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("damage", MinMaxBounds.Ints.ANY).forGetter(DamagePredicate::damage)
         )
         .apply(p_448614_, DamagePredicate::new)
   );

   @Override
   public boolean matches(DataComponentGetter p_391581_) {
      Integer integer = p_391581_.get(DataComponents.DAMAGE);
      if (integer == null) {
         return false;
      }

      int i = p_391581_.getOrDefault(DataComponents.MAX_DAMAGE, 0);
      return !this.durability.matches(i - integer) ? false : this.damage.matches(integer);
   }

   public static DamagePredicate durability(MinMaxBounds.Ints p_456807_) {
      return new DamagePredicate(p_456807_, MinMaxBounds.Ints.ANY);
   }
}
