package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public record PotDecorations(
   Optional<ItemStackTemplate> back, Optional<ItemStackTemplate> left, Optional<ItemStackTemplate> right, Optional<ItemStackTemplate> front
) implements TooltipProvider {
   public static final PotDecorations EMPTY = new PotDecorations(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
   public static final Codec<PotDecorations> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ItemStackTemplate.CODEC.optionalFieldOf("back").forGetter(PotDecorations::back),
            ItemStackTemplate.CODEC.optionalFieldOf("left").forGetter(PotDecorations::left),
            ItemStackTemplate.CODEC.optionalFieldOf("right").forGetter(PotDecorations::right),
            ItemStackTemplate.CODEC.optionalFieldOf("front").forGetter(PotDecorations::front)
         )
         .apply(i, PotDecorations::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PotDecorations> STREAM_CODEC = StreamCodec.composite(
      ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PotDecorations::back,
      ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PotDecorations::left,
      ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PotDecorations::right,
      ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PotDecorations::front,
      PotDecorations::new
   );

   @Override
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      if (!this.equals(EMPTY)) {
         consumer.accept(CommonComponents.EMPTY);
         addSideDetailsToTooltip(consumer, this.front);
         addSideDetailsToTooltip(consumer, this.left);
         addSideDetailsToTooltip(consumer, this.right);
         addSideDetailsToTooltip(consumer, this.back);
      }
   }

   private static void addSideDetailsToTooltip(final Consumer<Component> consumer, final Optional<ItemStackTemplate> side) {
      side.ifPresent(itemStackTemplate -> consumer.accept(itemStackTemplate.create().getHoverName().plainCopy().withStyle(ChatFormatting.GRAY)));
   }
}
