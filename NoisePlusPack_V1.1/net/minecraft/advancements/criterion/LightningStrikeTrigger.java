package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.loot.LootContext;

public class LightningStrikeTrigger extends SimpleCriterionTrigger<LightningStrikeTrigger.TriggerInstance> {
   @Override
   public Codec<LightningStrikeTrigger.TriggerInstance> codec() {
      return LightningStrikeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_459784_, LightningBolt p_457155_, List<Entity> p_454012_) {
      List<LootContext> list = p_454012_.stream().map(p_454945_ -> EntityPredicate.createContext(p_459784_, p_454945_)).collect(Collectors.toList());
      LootContext lootcontext = EntityPredicate.createContext(p_459784_, p_457155_);
      this.trigger(p_459784_, p_452355_ -> p_452355_.matches(lootcontext, list));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> lightning, Optional<ContextAwarePredicate> bystander)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<LightningStrikeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_460857_ -> p_460857_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(LightningStrikeTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("lightning").forGetter(LightningStrikeTrigger.TriggerInstance::lightning),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("bystander").forGetter(LightningStrikeTrigger.TriggerInstance::bystander)
            )
            .apply(p_460857_, LightningStrikeTrigger.TriggerInstance::new)
      );

      public static Criterion<LightningStrikeTrigger.TriggerInstance> lightningStrike(Optional<EntityPredicate> p_452070_, Optional<EntityPredicate> p_455983_) {
         return CriteriaTriggers.LIGHTNING_STRIKE
            .createCriterion(new LightningStrikeTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_452070_), EntityPredicate.wrap(p_455983_)));
      }

      public boolean matches(LootContext p_455037_, List<LootContext> p_453404_) {
         return this.lightning.isPresent() && !this.lightning.get().matches(p_455037_)
            ? false
            : !this.bystander.isPresent() || !p_453404_.stream().noneMatch(this.bystander.get()::matches);
      }

      @Override
      public void validate(CriterionValidator p_451992_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_451992_);
         p_451992_.validateEntity(this.lightning, "lightning");
         p_451992_.validateEntity(this.bystander, "bystander");
      }
   }
}
