package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class UsedTotemTrigger extends SimpleCriterionTrigger<UsedTotemTrigger.TriggerInstance> {
   @Override
   public Codec<UsedTotemTrigger.TriggerInstance> codec() {
      return UsedTotemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final ItemStack itemStack) {
      this.trigger(player, t -> t.matches(itemStack));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<UsedTotemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(UsedTotemTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(UsedTotemTrigger.TriggerInstance::item)
            )
            .apply(i, UsedTotemTrigger.TriggerInstance::new)
      );

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(final ItemPredicate item) {
         return CriteriaTriggers.USED_TOTEM.createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(item)));
      }

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(final HolderGetter<Item> items, final ItemLike itemlike) {
         return CriteriaTriggers.USED_TOTEM
            .createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(items, itemlike).build())));
      }

      public boolean matches(final ItemStack itemStack) {
         return this.item.isEmpty() || this.item.get().test(itemStack);
      }
   }
}
