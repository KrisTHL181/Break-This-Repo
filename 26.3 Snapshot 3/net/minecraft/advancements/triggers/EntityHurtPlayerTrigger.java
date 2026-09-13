package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.DamagePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class EntityHurtPlayerTrigger extends SimpleCriterionTrigger<EntityHurtPlayerTrigger.TriggerInstance> {
   @Override
   public Codec<EntityHurtPlayerTrigger.TriggerInstance> codec() {
      return EntityHurtPlayerTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final DamageSource source, final float originalDamage, final float actualDamage, final boolean blocked) {
      this.trigger(player, t -> t.matches(player, source, originalDamage, actualDamage, blocked));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<DamagePredicate> damage) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EntityHurtPlayerTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EntityHurtPlayerTrigger.TriggerInstance::player),
               DamagePredicate.CODEC.optionalFieldOf("damage").forGetter(EntityHurtPlayerTrigger.TriggerInstance::damage)
            )
            .apply(i, EntityHurtPlayerTrigger.TriggerInstance::new)
      );

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer() {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(final DamagePredicate damage) {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(damage)));
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(final DamagePredicate.Builder damage) {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(damage.build())));
      }

      public boolean matches(final ServerPlayer player, final DamageSource source, final float originalDamage, final float actualDamage, final boolean blocked) {
         return !this.damage.isPresent() || this.damage.get().matches(player, source, originalDamage, actualDamage, blocked);
      }
   }
}
