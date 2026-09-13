package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record UseRemainder(ItemStack convertInto) {
   public static final Codec<UseRemainder> CODEC = ItemStack.CODEC.xmap(UseRemainder::new, UseRemainder::convertInto);
   public static final StreamCodec<RegistryFriendlyByteBuf, UseRemainder> STREAM_CODEC = StreamCodec.composite(
      ItemStack.STREAM_CODEC, UseRemainder::convertInto, UseRemainder::new
   );

   public ItemStack convertIntoRemainder(ItemStack p_361599_, int p_362849_, boolean p_365138_, UseRemainder.OnExtraCreatedRemainder p_363866_) {
      if (p_365138_) {
         return p_361599_;
      }

      if (p_361599_.getCount() >= p_362849_) {
         return p_361599_;
      }

      ItemStack itemstack = this.convertInto.copy();
      if (p_361599_.isEmpty()) {
         return itemstack;
      }

      p_363866_.apply(itemstack);
      return p_361599_;
   }

   @Override
   public boolean equals(Object p_361701_) {
      if (this == p_361701_) {
         return true;
      } else if (p_361701_ != null && this.getClass() == p_361701_.getClass()) {
         UseRemainder useremainder = (UseRemainder)p_361701_;
         return ItemStack.matches(this.convertInto, useremainder.convertInto);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return ItemStack.hashItemAndComponents(this.convertInto);
   }

   @FunctionalInterface
   public interface OnExtraCreatedRemainder {
      void apply(ItemStack var1);
   }
}
