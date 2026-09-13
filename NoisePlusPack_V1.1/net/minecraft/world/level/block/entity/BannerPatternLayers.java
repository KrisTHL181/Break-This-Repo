package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.slf4j.Logger;

public record BannerPatternLayers(List<BannerPatternLayers.Layer> layers) implements TooltipProvider {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final BannerPatternLayers EMPTY = new BannerPatternLayers(List.of());
   public static final Codec<BannerPatternLayers> CODEC = BannerPatternLayers.Layer.CODEC.listOf().xmap(BannerPatternLayers::new, BannerPatternLayers::layers);
   public static final StreamCodec<RegistryFriendlyByteBuf, BannerPatternLayers> STREAM_CODEC = BannerPatternLayers.Layer.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(BannerPatternLayers::new, BannerPatternLayers::layers);

   public BannerPatternLayers removeLast() {
      return new BannerPatternLayers(List.copyOf(this.layers.subList(0, this.layers.size() - 1)));
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_395743_, Consumer<Component> p_392987_, TooltipFlag p_392065_, DataComponentGetter p_394991_) {
      for (int i = 0; i < Math.min(this.layers().size(), 6); i++) {
         p_392987_.accept(this.layers().get(i).description().withStyle(ChatFormatting.GRAY));
      }
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<BannerPatternLayers.Layer> layers = ImmutableList.builder();

      @Deprecated
      public BannerPatternLayers.Builder addIfRegistered(HolderGetter<BannerPattern> p_335943_, ResourceKey<BannerPattern> p_334059_, DyeColor p_331295_) {
         Optional<Holder.Reference<BannerPattern>> optional = p_335943_.get(p_334059_);
         if (optional.isEmpty()) {
            BannerPatternLayers.LOGGER.warn("Unable to find banner pattern with id: '{}'", p_334059_.identifier());
            return this;
         } else {
            return this.add(optional.get(), p_331295_);
         }
      }

      public BannerPatternLayers.Builder add(Holder<BannerPattern> p_333687_, DyeColor p_331527_) {
         return this.add(new BannerPatternLayers.Layer(p_333687_, p_331527_));
      }

      public BannerPatternLayers.Builder add(BannerPatternLayers.Layer p_329666_) {
         this.layers.add(p_329666_);
         return this;
      }

      public BannerPatternLayers.Builder addAll(BannerPatternLayers p_335609_) {
         this.layers.addAll(p_335609_.layers);
         return this;
      }

      public BannerPatternLayers build() {
         return new BannerPatternLayers(this.layers.build());
      }
   }

   public record Layer(Holder<BannerPattern> pattern, DyeColor color) {
      public static final Codec<BannerPatternLayers.Layer> CODEC = RecordCodecBuilder.create(
         p_332626_ -> p_332626_.group(
               BannerPattern.CODEC.fieldOf("pattern").forGetter(BannerPatternLayers.Layer::pattern),
               DyeColor.CODEC.fieldOf("color").forGetter(BannerPatternLayers.Layer::color)
            )
            .apply(p_332626_, BannerPatternLayers.Layer::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, BannerPatternLayers.Layer> STREAM_CODEC = StreamCodec.composite(
         BannerPattern.STREAM_CODEC,
         BannerPatternLayers.Layer::pattern,
         DyeColor.STREAM_CODEC,
         BannerPatternLayers.Layer::color,
         BannerPatternLayers.Layer::new
      );

      public MutableComponent description() {
         String s = this.pattern.value().translationKey();
         return Component.translatable(s + "." + this.color.getName());
      }
   }
}
