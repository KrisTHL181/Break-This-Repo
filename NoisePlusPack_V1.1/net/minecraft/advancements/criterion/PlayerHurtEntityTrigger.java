package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerHurtEntityTrigger extends SimpleCriterionTrigger<PlayerHurtEntityTrigger.TriggerInstance> {
   @Override
   public Codec<PlayerHurtEntityTrigger.TriggerInstance> codec() {
      return PlayerHurtEntityTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_453345_, Entity p_454394_, DamageSource p_459266_, float p_458629_, float p_454780_, boolean p_454981_) {
      LootContext lootcontext = EntityPredicate.createContext(p_453345_, p_454394_);
      this.trigger(p_453345_, p_456579_ -> p_456579_.matches(p_453345_, lootcontext, p_459266_, p_458629_, p_454780_, p_454981_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<DamagePredicate> damage, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PlayerHurtEntityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_452178_ -> p_452178_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerHurtEntityTrigger.TriggerInstance::player),
               DamagePredicate.CODEC.optionalFieldOf("damage").forGetter(PlayerHurtEntityTrigger.TriggerInstance::damage),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(PlayerHurtEntityTrigger.TriggerInstance::entity)
            )
            .apply(p_452178_, PlayerHurtEntityTrigger.TriggerInstance::new)
      );

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity() {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(Optional<DamagePredicate> p_453301_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY.createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), p_453301_, Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(DamagePredicate.Builder p_457757_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(p_457757_.build()), Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(Optional<EntityPredicate> p_458465_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), EntityPredicate.wrap(p_458465_)));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(
         Optional<DamagePredicate> p_456834_, Optional<EntityPredicate> p_454608_
      ) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), p_456834_, EntityPredicate.wrap(p_454608_)));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(DamagePredicate.Builder p_458364_, Optional<EntityPredicate> p_452037_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(p_458364_.build()), EntityPredicate.wrap(p_452037_)));
      }

      public boolean matches(ServerPlayer p_452308_, LootContext p_458715_, DamageSource p_454488_, float p_450839_, float p_451012_, boolean p_456500_) {
         return this.damage.isPresent() && !this.damage.get().matches(p_452308_, p_454488_, p_450839_, p_451012_, p_456500_)
            ? false
            : !this.entity.isPresent() || this.entity.get().matches(p_458715_);
      }

      @Override
      public void validate(CriterionValidator p_450347_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_450347_);
         p_450347_.validateEntity(this.entity, "entity");
      }
   }
}
