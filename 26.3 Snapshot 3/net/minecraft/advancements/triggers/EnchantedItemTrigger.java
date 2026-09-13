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

public class EnchantedItemTrigger extends SimpleCriterionTrigger<EnchantedItemTrigger.TriggerInstance> {
   @Override
   public Codec<EnchantedItemTrigger.TriggerInstance> codec() {
      return EnchantedItemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final ItemStack itemStack, final int levels) {
      this.trigger(player, t -> t.matches(itemStack, levels));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, MinMaxBounds.Ints levels)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EnchantedItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EnchantedItemTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(EnchantedItemTrigger.TriggerInstance::item),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("levels", MinMaxBounds.Ints.ANY).forGetter(EnchantedItemTrigger.TriggerInstance::levels)
            )
            .apply(i, EnchantedItemTrigger.TriggerInstance::new)
      );

      public static Criterion<EnchantedItemTrigger.TriggerInstance> enchantedItem() {
         return CriteriaTriggers.ENCHANTED_ITEM
            .createCriterion(new EnchantedItemTrigger.TriggerInstance(Optional.empty(), Optional.empty(), MinMaxBounds.Ints.ANY));
      }

      public boolean matches(final ItemStack itemStack, final int levels) {
         return this.item.isPresent() && !this.item.get().test(itemStack) ? false : this.levels.matches(levels);
      }
   }
}
