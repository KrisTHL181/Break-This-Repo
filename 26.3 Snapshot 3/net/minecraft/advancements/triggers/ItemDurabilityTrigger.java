package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilityTrigger extends SimpleCriterionTrigger<ItemDurabilityTrigger.TriggerInstance> {
   @Override
   public Codec<ItemDurabilityTrigger.TriggerInstance> codec() {
      return ItemDurabilityTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final ItemStack itemStack, final int newDurability) {
      this.trigger(player, t -> t.matches(itemStack, newDurability));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, MinMaxBounds.Ints durability, MinMaxBounds.Ints delta)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ItemDurabilityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemDurabilityTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ItemDurabilityTrigger.TriggerInstance::item),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("durability", MinMaxBounds.Ints.ANY).forGetter(ItemDurabilityTrigger.TriggerInstance::durability),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("delta", MinMaxBounds.Ints.ANY).forGetter(ItemDurabilityTrigger.TriggerInstance::delta)
            )
            .apply(i, ItemDurabilityTrigger.TriggerInstance::new)
      );

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(final Optional<ItemPredicate> item, final MinMaxBounds.Ints durability) {
         return changedDurability(Optional.empty(), item, durability);
      }

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(
         final Optional<ContextAwarePredicate> player, final Optional<ItemPredicate> item, final MinMaxBounds.Ints durability
      ) {
         return CriteriaTriggers.ITEM_DURABILITY_CHANGED
            .createCriterion(new ItemDurabilityTrigger.TriggerInstance(player, item, durability, MinMaxBounds.Ints.ANY));
      }

      public boolean matches(final ItemStack itemStack, final int newDurability) {
         if (this.item.isPresent() && !this.item.get().test(itemStack)) {
            return false;
         } else {
            return !this.durability.matches(itemStack.getMaxDamage() - newDurability) ? false : this.delta.matches(itemStack.getDamageValue() - newDurability);
         }
      }
   }
}
