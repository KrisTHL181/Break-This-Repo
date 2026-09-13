package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;

public record SulfurCubeContent(ItemStackTemplate absorbedBlockItemStack) implements TooltipProvider {
   public static final Codec<SulfurCubeContent> CODEC = ItemStackTemplate.CODEC.xmap(SulfurCubeContent::new, SulfurCubeContent::absorbedBlockItemStack);
   public static final StreamCodec<RegistryFriendlyByteBuf, SulfurCubeContent> STREAM_CODEC = ItemStackTemplate.STREAM_CODEC
      .map(SulfurCubeContent::new, SulfurCubeContent::absorbedBlockItemStack);

   public static SulfurCubeContent ofNonEmpty(final ItemStack itemStack) {
      return new SulfurCubeContent(ItemStackTemplate.fromNonEmptyStack(itemStack));
   }

   @Override
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      ItemStack currentStack = this.absorbedBlockItemStack.create();
      consumer.accept(
         Component.translatable("entity.minecraft.sulfur_cube.content", currentStack.getHoverName()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)
      );
   }
}
