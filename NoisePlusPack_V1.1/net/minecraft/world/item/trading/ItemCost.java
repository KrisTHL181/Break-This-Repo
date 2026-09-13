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
      p_390864_ -> p_390864_.group(
            Item.CODEC.fieldOf("id").forGetter(ItemCost::item),
            ExtraCodecs.POSITIVE_INT.fieldOf("count").orElse(1).forGetter(ItemCost::count),
            DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY).forGetter(ItemCost::components)
         )
         .apply(p_390864_, ItemCost::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemCost> STREAM_CODEC = StreamCodec.composite(
      Item.STREAM_CODEC, ItemCost::item, ByteBufCodecs.VAR_INT, ItemCost::count, DataComponentExactPredicate.STREAM_CODEC, ItemCost::components, ItemCost::new
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<ItemCost>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);

   public ItemCost(ItemLike p_333321_) {
      this(p_333321_, 1);
   }

   public ItemCost(ItemLike p_332783_, int p_331715_) {
      this(p_332783_.asItem().builtInRegistryHolder(), p_331715_, DataComponentExactPredicate.EMPTY);
   }

   public ItemCost(Holder<Item> p_334519_, int p_331202_, DataComponentExactPredicate p_397399_) {
      this(p_334519_, p_331202_, p_397399_, createStack(p_334519_, p_331202_, p_397399_));
   }

   public ItemCost withComponents(UnaryOperator<DataComponentExactPredicate.Builder> p_328625_) {
      return new ItemCost(this.item, this.count, p_328625_.apply(DataComponentExactPredicate.builder()).build());
   }

   private static ItemStack createStack(Holder<Item> p_329043_, int p_329370_, DataComponentExactPredicate p_393981_) {
      return new ItemStack(p_329043_, p_329370_, p_393981_.asPatch());
   }

   public boolean test(ItemStack p_331178_) {
      return p_331178_.is(this.item) && this.components.test(p_331178_);
   }
}
