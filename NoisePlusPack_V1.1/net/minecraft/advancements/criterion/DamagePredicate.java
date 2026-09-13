package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
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
      p_457407_ -> p_457407_.group(
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("dealt", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::dealtDamage),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("taken", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::takenDamage),
            EntityPredicate.CODEC.optionalFieldOf("source_entity").forGetter(DamagePredicate::sourceEntity),
            Codec.BOOL.optionalFieldOf("blocked").forGetter(DamagePredicate::blocked),
            DamageSourcePredicate.CODEC.optionalFieldOf("type").forGetter(DamagePredicate::type)
         )
         .apply(p_457407_, DamagePredicate::new)
   );

   public boolean matches(ServerPlayer p_453555_, DamageSource p_459899_, float p_455415_, float p_456869_, boolean p_450582_) {
      if (!this.dealtDamage.matches(p_455415_)) {
         return false;
      } else if (!this.takenDamage.matches(p_456869_)) {
         return false;
      } else if (this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(p_453555_, p_459899_.getEntity())) {
         return false;
      } else {
         return this.blocked.isPresent() && this.blocked.get() != p_450582_ ? false : !this.type.isPresent() || this.type.get().matches(p_453555_, p_459899_);
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

      public DamagePredicate.Builder dealtDamage(MinMaxBounds.Doubles p_454449_) {
         this.dealtDamage = p_454449_;
         return this;
      }

      public DamagePredicate.Builder takenDamage(MinMaxBounds.Doubles p_455288_) {
         this.takenDamage = p_455288_;
         return this;
      }

      public DamagePredicate.Builder sourceEntity(EntityPredicate p_456472_) {
         this.sourceEntity = Optional.of(p_456472_);
         return this;
      }

      public DamagePredicate.Builder blocked(Boolean p_457594_) {
         this.blocked = Optional.of(p_457594_);
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate p_457294_) {
         this.type = Optional.of(p_457294_);
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate.Builder p_451943_) {
         this.type = Optional.of(p_451943_.build());
         return this;
      }

      public DamagePredicate build() {
         return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
      }
   }
}
