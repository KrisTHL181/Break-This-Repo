package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilityTrigger extends SimpleCriterionTrigger<ItemDurabilityTrigger.TriggerInstance> {
   @Override
   public Codec<ItemDurabilityTrigger.TriggerInstance> codec() {
      return ItemDurabilityTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_458401_, ItemStack p_451075_, int p_455890_) {
      this.trigger(p_458401_, p_453276_ -> p_453276_.matches(p_451075_, p_455890_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, MinMaxBounds.Ints durability, MinMaxBounds.Ints delta)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ItemDurabilityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_453990_ -> p_453990_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemDurabilityTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ItemDurabilityTrigger.TriggerInstance::item),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("durability", MinMaxBounds.Ints.ANY).forGetter(ItemDurabilityTrigger.TriggerInstance::durability),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("delta", MinMaxBounds.Ints.ANY).forGetter(ItemDurabilityTrigger.TriggerInstance::delta)
            )
            .apply(p_453990_, ItemDurabilityTrigger.TriggerInstance::new)
      );

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(Optional<ItemPredicate> p_454874_, MinMaxBounds.Ints p_458805_) {
         return changedDurability(Optional.empty(), p_454874_, p_458805_);
      }

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(
         Optional<ContextAwarePredicate> p_454851_, Optional<ItemPredicate> p_453486_, MinMaxBounds.Ints p_453917_
      ) {
         return CriteriaTriggers.ITEM_DURABILITY_CHANGED
            .createCriterion(new ItemDurabilityTrigger.TriggerInstance(p_454851_, p_453486_, p_453917_, MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ItemStack p_451103_, int p_450897_) {
         if (this.item.isPresent() && !this.item.get().test(p_451103_)) {
            return false;
         } else {
            return !this.durability.matches(p_451103_.getMaxDamage() - p_450897_) ? false : this.delta.matches(p_451103_.getDamageValue() - p_450897_);
         }
      }
   }
}
