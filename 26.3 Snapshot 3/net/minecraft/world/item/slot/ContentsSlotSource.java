package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulators;

public class ContentsSlotSource extends TransformedSlotSource {
   public static final MapCodec<ContentsSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i).and(ContainerComponentManipulators.CODEC.fieldOf("component").forGetter(t -> t.component)).apply(i, ContentsSlotSource::new)
   );
   private final ContainerComponentManipulator<?> component;

   private ContentsSlotSource(final SlotSource slotSource, final ContainerComponentManipulator<?> component) {
      super(slotSource);
      this.component = component;
   }

   @Override
   public MapCodec<ContentsSlotSource> codec() {
      return MAP_CODEC;
   }

   @Override
   protected SlotCollection transform(final SlotCollection slots) {
      return slots.flatMap(this.component::getSlots);
   }
}
