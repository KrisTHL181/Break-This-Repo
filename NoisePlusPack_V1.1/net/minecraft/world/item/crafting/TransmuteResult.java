package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record TransmuteResult(Holder<Item> item, int count, DataComponentPatch components) {
   private static final Codec<TransmuteResult> FULL_CODEC = RecordCodecBuilder.create(
      p_396892_ -> p_396892_.group(
            Item.CODEC.fieldOf("id").forGetter(TransmuteResult::item),
            ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(TransmuteResult::count),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(TransmuteResult::components)
         )
         .apply(p_396892_, TransmuteResult::new)
   );
   public static final Codec<TransmuteResult> CODEC = Codec.withAlternative(FULL_CODEC, Item.CODEC, p_393238_ -> new TransmuteResult((Item)p_393238_.value()))
      .validate(TransmuteResult::validate);
   public static final StreamCodec<RegistryFriendlyByteBuf, TransmuteResult> STREAM_CODEC = StreamCodec.composite(
      Item.STREAM_CODEC,
      TransmuteResult::item,
      ByteBufCodecs.VAR_INT,
      TransmuteResult::count,
      DataComponentPatch.STREAM_CODEC,
      TransmuteResult::components,
      TransmuteResult::new
   );

   public TransmuteResult(Item p_395794_) {
      this(p_395794_.builtInRegistryHolder(), 1, DataComponentPatch.EMPTY);
   }

   private static DataResult<TransmuteResult> validate(TransmuteResult p_394619_) {
      return ItemStack.validateStrict(new ItemStack(p_394619_.item, p_394619_.count, p_394619_.components)).map(p_391171_ -> p_394619_);
   }

   public ItemStack apply(ItemStack p_396143_) {
      ItemStack itemstack = p_396143_.transmuteCopy(this.item.value(), this.count);
      itemstack.applyComponents(this.components);
      return itemstack;
   }

   public boolean isResultUnchanged(ItemStack p_398032_) {
      ItemStack itemstack = this.apply(p_398032_);
      return itemstack.getCount() == 1 && ItemStack.isSameItemSameComponents(p_398032_, itemstack);
   }

   public SlotDisplay display() {
      return new SlotDisplay.ItemStackSlotDisplay(new ItemStack(this.item, this.count, this.components));
   }
}
