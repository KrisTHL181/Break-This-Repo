package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;

public class TradeTrigger extends SimpleCriterionTrigger<TradeTrigger.TriggerInstance> {
   @Override
   public Codec<TradeTrigger.TriggerInstance> codec() {
      return TradeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final AbstractVillager villager, final ItemStack itemStack) {
      LootContext villagerContext = EntityPredicate.createContext(player, villager);
      this.trigger(player, t -> t.matches(villagerContext, itemStack));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> villager, Optional<ItemPredicate> item)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<TradeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TradeTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("villager").forGetter(TradeTrigger.TriggerInstance::villager),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TradeTrigger.TriggerInstance::item)
            )
            .apply(i, TradeTrigger.TriggerInstance::new)
      );

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager() {
         return CriteriaTriggers.TRADE.createCriterion(new TradeTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager(final EntityPredicate.Builder player) {
         return CriteriaTriggers.TRADE
            .createCriterion(new TradeTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(player)), Optional.empty(), Optional.empty()));
      }

      public boolean matches(final LootContext villager, final ItemStack itemStack) {
         return this.villager.isPresent() && !this.villager.get().matches(villager) ? false : !this.item.isPresent() || this.item.get().test(itemStack);
      }

      @Override
      public void validate(final ValidationContextSource validator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(validator);
         Validatable.validate(validator.entityContext(), "villager", this.villager);
      }
   }
}
