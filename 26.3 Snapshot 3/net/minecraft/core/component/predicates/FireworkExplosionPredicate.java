package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.FireworkExplosion;

public record FireworkExplosionPredicate(FireworkExplosionPredicate.FireworkPredicate predicate) implements SingleComponentItemPredicate<FireworkExplosion> {
   public static final Codec<FireworkExplosionPredicate> CODEC = FireworkExplosionPredicate.FireworkPredicate.CODEC
      .xmap(FireworkExplosionPredicate::new, FireworkExplosionPredicate::predicate);

   @Override
   public DataComponentType<FireworkExplosion> componentType() {
      return DataComponents.FIREWORK_EXPLOSION;
   }

   public boolean matches(final FireworkExplosion value) {
      return this.predicate.test(value);
   }

   public record FireworkPredicate(Optional<FireworkExplosion.Shape> shape, Optional<Boolean> twinkle, Optional<Boolean> trail)
      implements Predicate<FireworkExplosion> {
      public static final Codec<FireworkExplosionPredicate.FireworkPredicate> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               FireworkExplosion.Shape.CODEC.optionalFieldOf("shape").forGetter(FireworkExplosionPredicate.FireworkPredicate::shape),
               Codec.BOOL.optionalFieldOf("has_twinkle").forGetter(FireworkExplosionPredicate.FireworkPredicate::twinkle),
               Codec.BOOL.optionalFieldOf("has_trail").forGetter(FireworkExplosionPredicate.FireworkPredicate::trail)
            )
            .apply(i, FireworkExplosionPredicate.FireworkPredicate::new)
      );

      public boolean test(final FireworkExplosion fireworkExplosion) {
         if (this.shape.isPresent() && this.shape.get() != fireworkExplosion.shape()) {
            return false;
         } else {
            return this.twinkle.isPresent() && this.twinkle.get() != fireworkExplosion.hasTwinkle()
               ? false
               : !this.trail.isPresent() || this.trail.get() == fireworkExplosion.hasTrail();
         }
      }
   }
}
