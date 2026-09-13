package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ItemCost(Holder<Item> item, int count, DataComponentExactPredicate components, ItemStack itemStack) {
   public static final Codec<ItemCost> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Item.CODEC.fieldOf("id").forGetter(ItemCost::item),
            ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.POSITIVE_INT, "count", 1).forGetter(ItemCost::count),
            DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY).forGetter(ItemCost::components)
         )
         .apply(i, ItemCost::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemCost> STREAM_CODEC = StreamCodec.composite(
      Item.STREAM_CODEC, ItemCost::item, ByteBufCodecs.VAR_INT, ItemCost::count, DataComponentExactPredicate.STREAM_CODEC, ItemCost::components, ItemCost::new
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<ItemCost>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);

   public ItemCost(final ItemLike item) {
      this(item, 1);
   }

   public ItemCost(final ItemLike item, final int count) {
      this(item.asItem().builtInRegistryHolder(), count, DataComponentExactPredicate.EMPTY);
   }

   public ItemCost(final Holder<Item> item, final int count, final DataComponentExactPredicate components) {
      this(item, count, components, createStack(item, count, components));
   }

   public ItemCost withComponents(final UnaryOperator<DataComponentExactPredicate.Builder> components) {
      return new ItemCost(this.item, this.count, components.apply(DataComponentExactPredicate.builder()).build());
   }

   private static ItemStack createStack(final Holder<Item> item, final int count, final DataComponentExactPredicate components) {
      return new ItemStack(item, count, components.asPatch());
   }

   public boolean test(final ItemStack itemStack) {
      return itemStack.is(this.item) && this.components.test(itemStack);
   }
}
