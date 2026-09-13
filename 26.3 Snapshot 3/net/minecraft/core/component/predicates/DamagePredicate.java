package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;

public record DamagePredicate(MinMaxBounds.Ints durability, MinMaxBounds.Ints damage) implements DataComponentPredicate {
   public static final Codec<DamagePredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            MinMaxBounds.Ints.CODEC.optionalFieldOf("durability", MinMaxBounds.Ints.ANY).forGetter(DamagePredicate::durability),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("damage", MinMaxBounds.Ints.ANY).forGetter(DamagePredicate::damage)
         )
         .apply(i, DamagePredicate::new)
   );

   @Override
   public boolean matches(final DataComponentGetter components) {
      Integer damage = components.get(DataComponents.DAMAGE);
      if (damage == null) {
         return false;
      }

      int maxDamage = components.getOrDefault(DataComponents.MAX_DAMAGE, 0);
      return !this.durability.matches(maxDamage - damage) ? false : this.damage.matches(damage);
   }

   public static DamagePredicate durability(final MinMaxBounds.Ints range) {
      return new DamagePredicate(range, MinMaxBounds.Ints.ANY);
   }
}
