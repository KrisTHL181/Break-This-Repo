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
      public boolean matches(ItemStack p_391832_, HashedPatchMap.HashGenerator p_391539_) {
         return p_391832_.isEmpty();
      }
   };
   StreamCodec<RegistryFriendlyByteBuf, HashedStack> STREAM_CODEC = ByteBufCodecs.optional(HashedStack.ActualItem.STREAM_CODEC)
      .map(
         p_394313_ -> (HashedStack)DataFixUtils.orElse(p_394313_, EMPTY),
         p_394053_ -> p_394053_ instanceof HashedStack.ActualItem hashedstack$actualitem ? Optional.of(hashedstack$actualitem) : Optional.empty()
      );

   boolean matches(ItemStack var1, HashedPatchMap.HashGenerator var2);

   static HashedStack create(ItemStack p_394077_, HashedPatchMap.HashGenerator p_391374_) {
      return p_394077_.isEmpty()
         ? EMPTY
         : new HashedStack.ActualItem(p_394077_.getItemHolder(), p_394077_.getCount(), HashedPatchMap.create(p_394077_.getComponentsPatch(), p_391374_));
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
      public boolean matches(ItemStack p_397217_, HashedPatchMap.HashGenerator p_395066_) {
         if (this.count != p_397217_.getCount()) {
            return false;
         } else {
            return !this.item.equals(p_397217_.getItemHolder()) ? false : this.components.matches(p_397217_.getComponentsPatch(), p_395066_);
         }
      }
   }
}
