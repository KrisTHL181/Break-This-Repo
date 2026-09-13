package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class TradeTrigger extends SimpleCriterionTrigger<TradeTrigger.TriggerInstance> {
   @Override
   public Codec<TradeTrigger.TriggerInstance> codec() {
      return TradeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_457456_, AbstractVillager p_460970_, ItemStack p_454044_) {
      LootContext lootcontext = EntityPredicate.createContext(p_457456_, p_460970_);
      this.trigger(p_457456_, p_459792_ -> p_459792_.matches(lootcontext, p_454044_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> villager, Optional<ItemPredicate> item)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<TradeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_460650_ -> p_460650_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TradeTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("villager").forGetter(TradeTrigger.TriggerInstance::villager),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TradeTrigger.TriggerInstance::item)
            )
            .apply(p_460650_, TradeTrigger.TriggerInstance::new)
      );

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager() {
         return CriteriaTriggers.TRADE.createCriterion(new TradeTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager(EntityPredicate.Builder p_451328_) {
         return CriteriaTriggers.TRADE
            .createCriterion(new TradeTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_451328_)), Optional.empty(), Optional.empty()));
      }

      public boolean matches(LootContext p_459485_, ItemStack p_452587_) {
         return this.villager.isPresent() && !this.villager.get().matches(p_459485_) ? false : !this.item.isPresent() || this.item.get().test(p_452587_);
      }

      @Override
      public void validate(CriterionValidator p_455869_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_455869_);
         p_455869_.validateEntity(this.villager, "villager");
      }
   }
}
