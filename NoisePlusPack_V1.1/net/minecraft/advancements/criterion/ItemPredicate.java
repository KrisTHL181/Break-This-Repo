package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ItemPredicate(Optional<HolderSet<Item>> items, MinMaxBounds.Ints count, DataComponentMatchers components) implements Predicate<ItemStack> {
   public static final Codec<ItemPredicate> CODEC = RecordCodecBuilder.create(
      p_458117_ -> p_458117_.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(ItemPredicate::items),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("count", MinMaxBounds.Ints.ANY).forGetter(ItemPredicate::count),
            DataComponentMatchers.CODEC.forGetter(ItemPredicate::components)
         )
         .apply(p_458117_, ItemPredicate::new)
   );

   public boolean test(ItemStack p_455424_) {
      if (this.items.isPresent() && !p_455424_.is(this.items.get())) {
         return false;
      } else {
         return !this.count.matches(p_455424_.getCount()) ? false : this.components.test(p_455424_);
      }
   }

   public static class Builder {
      private Optional<HolderSet<Item>> items = Optional.empty();
      private MinMaxBounds.Ints count = MinMaxBounds.Ints.ANY;
      private DataComponentMatchers components = DataComponentMatchers.ANY;

      public static ItemPredicate.Builder item() {
         return new ItemPredicate.Builder();
      }

      public ItemPredicate.Builder of(HolderGetter<Item> p_457000_, ItemLike... p_450431_) {
         this.items = Optional.of(HolderSet.direct(p_452445_ -> p_452445_.asItem().builtInRegistryHolder(), p_450431_));
         return this;
      }

      public ItemPredicate.Builder of(HolderGetter<Item> p_459920_, TagKey<Item> p_455624_) {
         this.items = Optional.of(p_459920_.getOrThrow(p_455624_));
         return this;
      }

      public ItemPredicate.Builder withCount(MinMaxBounds.Ints p_459629_) {
         this.count = p_459629_;
         return this;
      }

      public ItemPredicate.Builder withComponents(DataComponentMatchers p_459330_) {
         this.components = p_459330_;
         return this;
      }

      public ItemPredicate build() {
         return new ItemPredicate(this.items, this.count, this.components);
      }
   }
}
