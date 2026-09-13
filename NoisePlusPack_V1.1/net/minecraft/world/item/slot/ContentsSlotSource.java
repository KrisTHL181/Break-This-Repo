package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulators;

public class ContentsSlotSource extends TransformedSlotSource {
   public static final MapCodec<ContentsSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_459646_ -> commonFields(p_459646_)
         .and(ContainerComponentManipulators.CODEC.fieldOf("component").forGetter(p_452947_ -> p_452947_.component))
         .apply(p_459646_, ContentsSlotSource::new)
   );
   private final ContainerComponentManipulator<?> component;

   private ContentsSlotSource(SlotSource p_456254_, ContainerComponentManipulator<?> p_457849_) {
      super(p_456254_);
      this.component = p_457849_;
   }

   @Override
   public MapCodec<ContentsSlotSource> codec() {
      return MAP_CODEC;
   }

   @Override
   protected SlotCollection transform(SlotCollection p_452694_) {
      return p_452694_.flatMap(this.component::getSlots);
   }
}
