package net.minecraft.network;

import com.mojang.datafixers.DataFixUtils;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface HashedStack {
   HashedStack EMPTY = new HashedStack() {
      @Override
      public String toString() {
         return "<empty>";
      }

      @Override
      public boolean matches(final ItemStack stack, final HashedPatchMap.HashGenerator hasher) {
         return stack.isEmpty();
      }
   };
   StreamCodec<RegistryFriendlyByteBuf, HashedStack> STREAM_CODEC = ByteBufCodecs.optional(HashedStack.ActualItem.STREAM_CODEC)
      .map(
         actualItem -> (HashedStack)DataFixUtils.orElse(actualItem, EMPTY),
         hashedStack -> hashedStack instanceof HashedStack.ActualItem actualItem ? Optional.of(actualItem) : Optional.empty()
      );

   boolean matches(ItemStack stack, HashedPatchMap.HashGenerator hasher);

   static HashedStack create(final ItemStack itemStack, final HashedPatchMap.HashGenerator hasher) {
      return itemStack.isEmpty()
         ? EMPTY
         : new HashedStack.ActualItem(itemStack.typeHolder(), itemStack.getCount(), HashedPatchMap.create(itemStack.getComponentsPatch(), hasher));
   }

   record ActualItem(Holder<Item> item, int count, HashedPatchMap components) implements HashedStack {
      public static final StreamCodec<RegistryFriendlyByteBuf, HashedStack.ActualItem> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.holderRegistry(Registries.ITEM),
         HashedStack.ActualItem::item,
         ByteBufCodecs.VAR_INT,
         HashedStack.ActualItem::count,
         HashedPatchMap.STREAM_CODEC,
         HashedStack.ActualItem::components,
         HashedStack.ActualItem::new
      );

      @Override
      public boolean matches(final ItemStack itemStack, final HashedPatchMap.HashGenerator hasher) {
         if (this.count != itemStack.getCount()) {
            return false;
         } else {
            return !this.item.equals(itemStack.typeHolder()) ? false : this.components.matches(itemStack.getComponentsPatch(), hasher);
         }
      }
   }
}
