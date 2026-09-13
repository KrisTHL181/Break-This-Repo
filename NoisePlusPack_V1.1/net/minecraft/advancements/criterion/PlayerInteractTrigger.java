package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerInteractTrigger extends SimpleCriterionTrigger<PlayerInteractTrigger.TriggerInstance> {
   @Override
   public Codec<PlayerInteractTrigger.TriggerInstance> codec() {
      return PlayerInteractTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_457560_, ItemStack p_451216_, Entity p_453968_) {
      LootContext lootcontext = EntityPredicate.createContext(p_457560_, p_453968_);
      this.trigger(p_457560_, p_451237_ -> p_451237_.matches(p_451216_, lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PlayerInteractTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_452492_ -> p_452492_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerInteractTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(PlayerInteractTrigger.TriggerInstance::item),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(PlayerInteractTrigger.TriggerInstance::entity)
            )
            .apply(p_452492_, PlayerInteractTrigger.TriggerInstance::new)
      );

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(
         Optional<ContextAwarePredicate> p_458015_, ItemPredicate.Builder p_460827_, Optional<ContextAwarePredicate> p_457831_
      ) {
         return CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY
            .createCriterion(new PlayerInteractTrigger.TriggerInstance(p_458015_, Optional.of(p_460827_.build()), p_457831_));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> equipmentSheared(
         Optional<ContextAwarePredicate> p_456933_, ItemPredicate.Builder p_450906_, Optional<ContextAwarePredicate> p_455701_
      ) {
         return CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT
            .createCriterion(new PlayerInteractTrigger.TriggerInstance(p_456933_, Optional.of(p_450906_.build()), p_455701_));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> equipmentSheared(
         ItemPredicate.Builder p_450584_, Optional<ContextAwarePredicate> p_452973_
      ) {
         return CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT
            .createCriterion(new PlayerInteractTrigger.TriggerInstance(Optional.empty(), Optional.of(p_450584_.build()), p_452973_));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(
         ItemPredicate.Builder p_457490_, Optional<ContextAwarePredicate> p_457404_
      ) {
         return itemUsedOnEntity(Optional.empty(), p_457490_, p_457404_);
      }

      public boolean matches(ItemStack p_454167_, LootContext p_453190_) {
         return this.item.isPresent() && !this.item.get().test(p_454167_) ? false : this.entity.isEmpty() || this.entity.get().matches(p_453190_);
      }

      @Override
      public void validate(CriterionValidator p_460099_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_460099_);
         p_460099_.validateEntity(this.entity, "entity");
      }
   }
}
