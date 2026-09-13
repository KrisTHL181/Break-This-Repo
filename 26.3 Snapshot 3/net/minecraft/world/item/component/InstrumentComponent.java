package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public record InstrumentComponent(Holder<Instrument> instrument) implements TooltipProvider {
   public static final Codec<InstrumentComponent> CODEC = Instrument.CODEC.xmap(InstrumentComponent::new, InstrumentComponent::instrument);
   public static final StreamCodec<RegistryFriendlyByteBuf, InstrumentComponent> STREAM_CODEC = Instrument.STREAM_CODEC
      .map(InstrumentComponent::new, InstrumentComponent::instrument);

   @Override
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      consumer.accept(ComponentUtils.mergeStyles(this.instrument.value().description(), Style.EMPTY.withColor(ChatFormatting.GRAY)));
   }
}
