package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public record InstrumentComponent(EitherHolder<Instrument> instrument) implements TooltipProvider {
   public static final Codec<InstrumentComponent> CODEC = EitherHolder.codec(Registries.INSTRUMENT, Instrument.CODEC)
      .xmap(InstrumentComponent::new, InstrumentComponent::instrument);
   public static final StreamCodec<RegistryFriendlyByteBuf, InstrumentComponent> STREAM_CODEC = EitherHolder.streamCodec(
         Registries.INSTRUMENT, Instrument.STREAM_CODEC
      )
      .map(InstrumentComponent::new, InstrumentComponent::instrument);

   public InstrumentComponent(Holder<Instrument> p_394572_) {
      this(new EitherHolder<>(p_394572_));
   }

   @Deprecated
   public InstrumentComponent(ResourceKey<Instrument> p_394091_) {
      this(new EitherHolder<>(p_394091_));
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_393568_, Consumer<Component> p_391170_, TooltipFlag p_393894_, DataComponentGetter p_397888_) {
      HolderLookup.Provider holderlookup$provider = p_393568_.registries();
      if (holderlookup$provider != null) {
         this.unwrap(holderlookup$provider).ifPresent(p_451163_ -> {
            Component component = ComponentUtils.mergeStyles(p_451163_.value().description(), Style.EMPTY.withColor(ChatFormatting.GRAY));
            p_391170_.accept(component);
         });
      }
   }

   public Optional<Holder<Instrument>> unwrap(HolderLookup.Provider p_395970_) {
      return this.instrument.unwrap(p_395970_);
   }
}
