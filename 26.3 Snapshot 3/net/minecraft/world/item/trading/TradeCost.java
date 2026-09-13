package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public record TradeCost(Holder<Item> item, NumberProvider count, DataComponentExactPredicate components) implements Validatable {
   public static final Codec<TradeCost> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Item.CODEC.fieldOf("id").forGetter(TradeCost::item),
            NumberProviders.DIRECT_CODEC.optionalFieldOf("count", ConstantValue.exactly(1.0F)).forGetter(TradeCost::count),
            DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY).forGetter(TradeCost::components)
         )
         .apply(i, TradeCost::new)
   );

   public TradeCost(final ItemLike item, final int count) {
      this(item.asItem().builtInRegistryHolder(), ConstantValue.exactly(count), DataComponentExactPredicate.EMPTY);
   }

   public TradeCost(final ItemLike item, final NumberProvider count) {
      this(item.asItem().builtInRegistryHolder(), count, DataComponentExactPredicate.EMPTY);
   }

   public ItemCost toItemCost(final LootContext lootContext, final int additionalCost) {
      int count = Mth.clamp(this.count().getInt(lootContext) + additionalCost, 0, this.item().value().getDefaultMaxStackSize());
      return new ItemCost(this.item(), count, this.components());
   }

   @Override
   public void validate(final ValidationContext context) {
      Validatable.validate(context, "count", this.count);
   }
}
