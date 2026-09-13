package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.criterion.SingleComponentItemPredicate;
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

   public boolean matches(FireworkExplosion p_397790_) {
      return this.predicate.test(p_397790_);
   }

   public record FireworkPredicate(Optional<FireworkExplosion.Shape> shape, Optional<Boolean> twinkle, Optional<Boolean> trail)
      implements Predicate<FireworkExplosion> {
      public static final Codec<FireworkExplosionPredicate.FireworkPredicate> CODEC = RecordCodecBuilder.create(
         p_393707_ -> p_393707_.group(
               FireworkExplosion.Shape.CODEC.optionalFieldOf("shape").forGetter(FireworkExplosionPredicate.FireworkPredicate::shape),
               Codec.BOOL.optionalFieldOf("has_twinkle").forGetter(FireworkExplosionPredicate.FireworkPredicate::twinkle),
               Codec.BOOL.optionalFieldOf("has_trail").forGetter(FireworkExplosionPredicate.FireworkPredicate::trail)
            )
            .apply(p_393707_, FireworkExplosionPredicate.FireworkPredicate::new)
      );

      public boolean test(FireworkExplosion p_391776_) {
         if (this.shape.isPresent() && this.shape.get() != p_391776_.shape()) {
            return false;
         } else {
            return this.twinkle.isPresent() && this.twinkle.get() != p_391776_.hasTwinkle()
               ? false
               : !this.trail.isPresent() || this.trail.get() == p_391776_.hasTrail();
         }
      }
   }
}
