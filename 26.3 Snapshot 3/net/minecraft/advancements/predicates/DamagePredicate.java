package net.minecraft.advancements.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public record DamagePredicate(
   MinMaxBounds.Doubles dealtDamage,
   MinMaxBounds.Doubles takenDamage,
   Optional<EntityPredicate> sourceEntity,
   Optional<Boolean> blocked,
   Optional<DamageSourcePredicate> type
) {
   public static final Codec<DamagePredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("dealt", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::dealtDamage),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("taken", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::takenDamage),
            EntityPredicate.CODEC.optionalFieldOf("source_entity").forGetter(DamagePredicate::sourceEntity),
            Codec.BOOL.optionalFieldOf("blocked").forGetter(DamagePredicate::blocked),
            DamageSourcePredicate.CODEC.optionalFieldOf("type").forGetter(DamagePredicate::type)
         )
         .apply(i, DamagePredicate::new)
   );

   public boolean matches(final ServerPlayer player, final DamageSource source, final float originalDamage, final float actualDamage, final boolean blocked) {
      if (!this.dealtDamage.matches(originalDamage)) {
         return false;
      } else if (!this.takenDamage.matches(actualDamage)) {
         return false;
      } else if (this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(player, source.getEntity())) {
         return false;
      } else {
         return this.blocked.isPresent() && this.blocked.get() != blocked ? false : !this.type.isPresent() || this.type.get().matches(player, source);
      }
   }

   public static class Builder {
      private MinMaxBounds.Doubles dealtDamage = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles takenDamage = MinMaxBounds.Doubles.ANY;
      private Optional<EntityPredicate> sourceEntity = Optional.empty();
      private Optional<Boolean> blocked = Optional.empty();
      private Optional<DamageSourcePredicate> type = Optional.empty();

      public static DamagePredicate.Builder damageInstance() {
         return new DamagePredicate.Builder();
      }

      public DamagePredicate.Builder dealtDamage(final MinMaxBounds.Doubles dealtDamage) {
         this.dealtDamage = dealtDamage;
         return this;
      }

      public DamagePredicate.Builder takenDamage(final MinMaxBounds.Doubles takenDamage) {
         this.takenDamage = takenDamage;
         return this;
      }

      public DamagePredicate.Builder sourceEntity(final EntityPredicate sourceEntity) {
         this.sourceEntity = Optional.of(sourceEntity);
         return this;
      }

      public DamagePredicate.Builder blocked(final Boolean blocked) {
         this.blocked = Optional.of(blocked);
         return this;
      }

      public DamagePredicate.Builder type(final DamageSourcePredicate type) {
         this.type = Optional.of(type);
         return this;
      }

      public DamagePredicate.Builder type(final DamageSourcePredicate.Builder type) {
         this.type = Optional.of(type.build());
         return this;
      }

      public DamagePredicate build() {
         return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
      }
   }
}
