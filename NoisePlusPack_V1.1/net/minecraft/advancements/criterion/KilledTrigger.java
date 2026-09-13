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

public class KilledTrigger extends SimpleCriterionTrigger<KilledTrigger.TriggerInstance> {
   @Override
   public Codec<KilledTrigger.TriggerInstance> codec() {
      return KilledTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_458666_, Entity p_459895_, DamageSource p_459179_) {
      LootContext lootcontext = EntityPredicate.createContext(p_458666_, p_459895_);
      this.trigger(p_458666_, p_460705_ -> p_460705_.matches(p_458666_, lootcontext, p_459179_));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entityPredicate, Optional<DamageSourcePredicate> killingBlow
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<KilledTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_456422_ -> p_456422_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(KilledTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(KilledTrigger.TriggerInstance::entityPredicate),
               DamageSourcePredicate.CODEC.optionalFieldOf("killing_blow").forGetter(KilledTrigger.TriggerInstance::killingBlow)
            )
            .apply(p_456422_, KilledTrigger.TriggerInstance::new)
      );

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> p_457374_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_457374_), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder p_460535_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_460535_)), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity() {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> p_456684_, Optional<DamageSourcePredicate> p_459766_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_456684_), p_459766_));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder p_458735_, Optional<DamageSourcePredicate> p_454857_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_458735_)), p_454857_));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> p_459161_, DamageSourcePredicate.Builder p_452390_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_459161_), Optional.of(p_452390_.build())));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder p_458654_, DamageSourcePredicate.Builder p_458315_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_458654_)), Optional.of(p_458315_.build())));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntityNearSculkCatalyst() {
         return CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> p_451730_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_451730_), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder p_455246_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_455246_)), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer() {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> p_459410_, Optional<DamageSourcePredicate> p_458781_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_459410_), p_458781_));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder p_461025_, Optional<DamageSourcePredicate> p_459046_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_461025_)), p_459046_));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> p_453996_, DamageSourcePredicate.Builder p_459349_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_453996_), Optional.of(p_459349_.build())));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder p_458958_, DamageSourcePredicate.Builder p_451255_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_458958_)), Optional.of(p_451255_.build())));
      }

      public boolean matches(ServerPlayer p_457519_, LootContext p_459216_, DamageSource p_457559_) {
         return this.killingBlow.isPresent() && !this.killingBlow.get().matches(p_457519_, p_457559_)
            ? false
            : this.entityPredicate.isEmpty() || this.entityPredicate.get().matches(p_459216_);
      }

      @Override
      public void validate(CriterionValidator p_455525_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_455525_);
         p_455525_.validateEntity(this.entityPredicate, "entity");
      }
   }
}
