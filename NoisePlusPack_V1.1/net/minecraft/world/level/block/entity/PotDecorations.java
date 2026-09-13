package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public record PotDecorations(Optional<Item> back, Optional<Item> left, Optional<Item> right, Optional<Item> front) implements TooltipProvider {
   public static final PotDecorations EMPTY = new PotDecorations(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
   public static final Codec<PotDecorations> CODEC = BuiltInRegistries.ITEM
      .byNameCodec()
      .sizeLimitedListOf(4)
      .xmap(PotDecorations::new, PotDecorations::ordered);
   public static final StreamCodec<RegistryFriendlyByteBuf, PotDecorations> STREAM_CODEC = ByteBufCodecs.registry(Registries.ITEM)
      .apply(ByteBufCodecs.list(4))
      .map(PotDecorations::new, PotDecorations::ordered);

   private PotDecorations(List<Item> p_331996_) {
      this(getItem(p_331996_, 0), getItem(p_331996_, 1), getItem(p_331996_, 2), getItem(p_331996_, 3));
   }

   public PotDecorations(Item p_335624_, Item p_333843_, Item p_334423_, Item p_332271_) {
      this(List.of(p_335624_, p_333843_, p_334423_, p_332271_));
   }

   private static Optional<Item> getItem(List<Item> p_329359_, int p_331055_) {
      if (p_331055_ >= p_329359_.size()) {
         return Optional.empty();
      }

      Item item = p_329359_.get(p_331055_);
      return item == Items.BRICK ? Optional.empty() : Optional.of(item);
   }

   public List<Item> ordered() {
      return Stream.of(this.back, this.left, this.right, this.front).map(p_330456_ -> p_330456_.orElse(Items.BRICK)).toList();
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_396913_, Consumer<Component> p_396999_, TooltipFlag p_393266_, DataComponentGetter p_394963_) {
      if (!this.equals(EMPTY)) {
         p_396999_.accept(CommonComponents.EMPTY);
         addSideDetailsToTooltip(p_396999_, this.front);
         addSideDetailsToTooltip(p_396999_, this.left);
         addSideDetailsToTooltip(p_396999_, this.right);
         addSideDetailsToTooltip(p_396999_, this.back);
      }
   }

   private static void addSideDetailsToTooltip(Consumer<Component> p_396489_, Optional<Item> p_391827_) {
      p_396489_.accept(new ItemStack(p_391827_.orElse(Items.BRICK), 1).getHoverName().plainCopy().withStyle(ChatFormatting.GRAY));
   }
}
