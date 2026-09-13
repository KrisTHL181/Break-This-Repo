package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public record ItemLore(List<Component> lines, List<Component> styledLines) implements TooltipProvider {
   public static final ItemLore EMPTY = new ItemLore(List.of());
   public static final int MAX_LINES = 256;
   private static final Style LORE_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true);
   public static final Codec<ItemLore> CODEC = ComponentSerialization.CODEC.sizeLimitedListOf(256).xmap(ItemLore::new, ItemLore::lines);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemLore> STREAM_CODEC = ComponentSerialization.STREAM_CODEC
      .apply(ByteBufCodecs.list(256))
      .map(ItemLore::new, ItemLore::lines);

   public ItemLore(List<Component> p_330285_) {
      this(p_330285_, Lists.transform(p_330285_, p_449819_ -> ComponentUtils.mergeStyles(p_449819_, LORE_STYLE)));
   }

   public ItemLore {
      if (lines.size() > 256) {
         throw new IllegalArgumentException("Got " + lines.size() + " lines, but maximum is 256");
      }
   }

   public ItemLore withLineAdded(Component p_328621_) {
      return new ItemLore(Util.copyAndAdd(this.lines, p_328621_));
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_329761_, Consumer<Component> p_332607_, TooltipFlag p_328590_, DataComponentGetter p_397979_) {
      this.styledLines.forEach(p_332607_);
   }
}
