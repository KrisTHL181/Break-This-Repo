package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class InventoryChangeTrigger extends SimpleCriterionTrigger<InventoryChangeTrigger.TriggerInstance> {
   @Override
   public Codec<InventoryChangeTrigger.TriggerInstance> codec() {
      return InventoryChangeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_453821_, Inventory p_460304_, ItemStack p_450940_) {
      int i = 0;
      int j = 0;
      int k = 0;

      for (int l = 0; l < p_460304_.getContainerSize(); l++) {
         ItemStack itemstack = p_460304_.getItem(l);
         if (itemstack.isEmpty()) {
            j++;
         } else {
            k++;
            if (itemstack.getCount() >= itemstack.getMaxStackSize()) {
               i++;
            }
         }
      }

      this.trigger(p_453821_, p_460304_, p_450940_, i, j, k);
   }

   private void trigger(ServerPlayer p_460633_, Inventory p_456815_, ItemStack p_458795_, int p_458119_, int p_461022_, int p_455280_) {
      this.trigger(p_460633_, p_457824_ -> p_457824_.matches(p_456815_, p_458795_, p_458119_, p_461022_, p_455280_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, InventoryChangeTrigger.TriggerInstance.Slots slots, List<ItemPredicate> items)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<InventoryChangeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_458694_ -> p_458694_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(InventoryChangeTrigger.TriggerInstance::player),
               InventoryChangeTrigger.TriggerInstance.Slots.CODEC
                  .optionalFieldOf("slots", InventoryChangeTrigger.TriggerInstance.Slots.ANY)
                  .forGetter(InventoryChangeTrigger.TriggerInstance::slots),
               ItemPredicate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(InventoryChangeTrigger.TriggerInstance::items)
            )
            .apply(p_458694_, InventoryChangeTrigger.TriggerInstance::new)
      );

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate.Builder... p_454856_) {
         return hasItems(Stream.of(p_454856_).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
      }

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate... p_455574_) {
         return CriteriaTriggers.INVENTORY_CHANGED
            .createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(p_455574_)));
      }

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemLike... p_454984_) {
         ItemPredicate[] aitempredicate = new ItemPredicate[p_454984_.length];

         for (int i = 0; i < p_454984_.length; i++) {
            aitempredicate[i] = new ItemPredicate(
               Optional.of(HolderSet.direct(p_454984_[i].asItem().builtInRegistryHolder())), MinMaxBounds.Ints.ANY, DataComponentMatchers.ANY
            );
         }

         return hasItems(aitempredicate);
      }

      public boolean matches(Inventory p_452601_, ItemStack p_454912_, int p_456871_, int p_451578_, int p_452452_) {
         if (!this.slots.matches(p_456871_, p_451578_, p_452452_)) {
            return false;
         }

         if (this.items.isEmpty()) {
            return true;
         }

         if (this.items.size() != 1) {
            List<ItemPredicate> list = new ObjectArrayList(this.items);
            int i = p_452601_.getContainerSize();

            for (int j = 0; j < i; j++) {
               if (list.isEmpty()) {
                  return true;
               }

               ItemStack itemstack = p_452601_.getItem(j);
               if (!itemstack.isEmpty()) {
                  list.removeIf(p_450382_ -> p_450382_.test(itemstack));
               }
            }

            return list.isEmpty();
         } else {
            return !p_454912_.isEmpty() && this.items.get(0).test(p_454912_);
         }
      }

      public record Slots(MinMaxBounds.Ints occupied, MinMaxBounds.Ints full, MinMaxBounds.Ints empty) {
         public static final Codec<InventoryChangeTrigger.TriggerInstance.Slots> CODEC = RecordCodecBuilder.create(
            p_454161_ -> p_454161_.group(
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("occupied", MinMaxBounds.Ints.ANY).forGetter(InventoryChangeTrigger.TriggerInstance.Slots::occupied),
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("full", MinMaxBounds.Ints.ANY).forGetter(InventoryChangeTrigger.TriggerInstance.Slots::full),
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("empty", MinMaxBounds.Ints.ANY).forGetter(InventoryChangeTrigger.TriggerInstance.Slots::empty)
               )
               .apply(p_454161_, InventoryChangeTrigger.TriggerInstance.Slots::new)
         );
         public static final InventoryChangeTrigger.TriggerInstance.Slots ANY = new InventoryChangeTrigger.TriggerInstance.Slots(
            MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY
         );

         public boolean matches(int p_454797_, int p_459524_, int p_455626_) {
            if (!this.full.matches(p_454797_)) {
               return false;
            } else {
               return !this.empty.matches(p_459524_) ? false : this.occupied.matches(p_455626_);
            }
         }
      }
   }
}
