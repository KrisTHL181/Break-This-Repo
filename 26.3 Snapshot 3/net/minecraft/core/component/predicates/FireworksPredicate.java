package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.CollectionPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

public record FireworksPredicate(
   Optional<CollectionPredicate<FireworkExplosion, FireworkExplosionPredicate.FireworkPredicate>> explosions, MinMaxBounds.Ints flightDuration
) implements SingleComponentItemPredicate<Fireworks> {
   public static final Codec<FireworksPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            CollectionPredicate.codec(FireworkExplosionPredicate.FireworkPredicate.CODEC)
               .optionalFieldOf("explosions")
               .forGetter(FireworksPredicate::explosions),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("flight_duration", MinMaxBounds.Ints.ANY).forGetter(FireworksPredicate::flightDuration)
         )
         .apply(i, FireworksPredicate::new)
   );

   @Override
   public DataComponentType<Fireworks> componentType() {
      return DataComponents.FIREWORKS;
   }

   public boolean matches(final Fireworks value) {
      return this.explosions.isPresent() && !this.explosions.get().test(value.explosions()) ? false : this.flightDuration.matches(value.flightDuration());
   }
}
