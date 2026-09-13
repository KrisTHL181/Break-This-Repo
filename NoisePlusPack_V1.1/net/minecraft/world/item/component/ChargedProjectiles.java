package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ChargedProjectiles implements TooltipProvider {
   public static final ChargedProjectiles EMPTY = new ChargedProjectiles(List.of());
   public static final Codec<ChargedProjectiles> CODEC = ItemStack.CODEC.listOf().xmap(ChargedProjectiles::new, p_333238_ -> p_333238_.items);
   public static final StreamCodec<RegistryFriendlyByteBuf, ChargedProjectiles> STREAM_CODEC = ItemStack.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(ChargedProjectiles::new, p_330449_ -> p_330449_.items);
   private final List<ItemStack> items;

   private ChargedProjectiles(List<ItemStack> p_328441_) {
      this.items = p_328441_;
   }

   public static ChargedProjectiles of(ItemStack p_330424_) {
      return new ChargedProjectiles(List.of(p_330424_.copy()));
   }

   public static ChargedProjectiles of(List<ItemStack> p_334351_) {
      return new ChargedProjectiles(List.copyOf(Lists.transform(p_334351_, ItemStack::copy)));
   }

   public boolean contains(Item p_329513_) {
      for (ItemStack itemstack : this.items) {
         if (itemstack.is(p_329513_)) {
            return true;
         }
      }

      return false;
   }

   public List<ItemStack> getItems() {
      return Lists.transform(this.items, ItemStack::copy);
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   @Override
   public boolean equals(Object p_332122_) {
      return this == p_332122_
         ? true
         : p_332122_ instanceof ChargedProjectiles chargedprojectiles && ItemStack.listMatches(this.items, chargedprojectiles.items);
   }

   @Override
   public int hashCode() {
      return ItemStack.hashStackList(this.items);
   }

   @Override
   public String toString() {
      return "ChargedProjectiles[items=" + this.items + "]";
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_391340_, Consumer<Component> p_393178_, TooltipFlag p_392958_, DataComponentGetter p_396521_) {
      ItemStack itemstack = null;
      int i = 0;

      for (ItemStack itemstack1 : this.items) {
         if (itemstack == null) {
            itemstack = itemstack1;
            i = 1;
         } else if (ItemStack.matches(itemstack, itemstack1)) {
            i++;
         } else {
            addProjectileTooltip(p_391340_, p_393178_, itemstack, i);
            itemstack = itemstack1;
            i = 1;
         }
      }

      if (itemstack != null) {
         addProjectileTooltip(p_391340_, p_393178_, itemstack, i);
      }
   }

   private static void addProjectileTooltip(Item.TooltipContext p_397038_, Consumer<Component> p_397040_, ItemStack p_398013_, int p_393234_) {
      if (p_393234_ == 1) {
         p_397040_.accept(Component.translatable("item.minecraft.crossbow.projectile.single", p_398013_.getDisplayName()));
      } else {
         p_397040_.accept(Component.translatable("item.minecraft.crossbow.projectile.multiple", p_393234_, p_398013_.getDisplayName()));
      }

      TooltipDisplay tooltipdisplay = p_398013_.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
      p_398013_.addDetailsToTooltip(
         p_397038_,
         tooltipdisplay,
         null,
         TooltipFlag.NORMAL,
         p_390820_ -> p_397040_.accept(Component.literal("  ").append(p_390820_).withStyle(ChatFormatting.GRAY))
      );
   }
}
