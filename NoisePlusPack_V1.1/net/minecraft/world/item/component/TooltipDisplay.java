package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import java.util.List;
import java.util.SequencedSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TooltipDisplay(boolean hideTooltip, SequencedSet<DataComponentType<?>> hiddenComponents) {
   private static final Codec<SequencedSet<DataComponentType<?>>> COMPONENT_SET_CODEC = DataComponentType.CODEC
      .listOf()
      .xmap(ReferenceLinkedOpenHashSet::new, List::copyOf);
   public static final Codec<TooltipDisplay> CODEC = RecordCodecBuilder.create(
      p_397434_ -> p_397434_.group(
            Codec.BOOL.optionalFieldOf("hide_tooltip", false).forGetter(TooltipDisplay::hideTooltip),
            COMPONENT_SET_CODEC.optionalFieldOf("hidden_components", ReferenceSortedSets.emptySet()).forGetter(TooltipDisplay::hiddenComponents)
         )
         .apply(p_397434_, TooltipDisplay::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TooltipDisplay> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      TooltipDisplay::hideTooltip,
      DataComponentType.STREAM_CODEC.apply(ByteBufCodecs.collection(ReferenceLinkedOpenHashSet::new)),
      TooltipDisplay::hiddenComponents,
      TooltipDisplay::new
   );
   public static final TooltipDisplay DEFAULT = new TooltipDisplay(false, ReferenceSortedSets.emptySet());

   public TooltipDisplay withHidden(DataComponentType<?> p_397345_, boolean p_396287_) {
      if (this.hiddenComponents.contains(p_397345_) == p_396287_) {
         return this;
      }

      SequencedSet<DataComponentType<?>> sequencedset = new ReferenceLinkedOpenHashSet(this.hiddenComponents);
      if (p_396287_) {
         sequencedset.add(p_397345_);
      } else {
         sequencedset.remove(p_397345_);
      }

      return new TooltipDisplay(this.hideTooltip, sequencedset);
   }

   public boolean shows(DataComponentType<?> p_397305_) {
      return !this.hideTooltip && !this.hiddenComponents.contains(p_397305_);
   }
}
