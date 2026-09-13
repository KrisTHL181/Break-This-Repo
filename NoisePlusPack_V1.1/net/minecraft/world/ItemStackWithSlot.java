package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public record ItemStackWithSlot(int slot, ItemStack stack) {
   public static final Codec<ItemStackWithSlot> CODEC = RecordCodecBuilder.create(
      p_406430_ -> p_406430_.group(
            ExtraCodecs.UNSIGNED_BYTE.fieldOf("Slot").orElse(0).forGetter(ItemStackWithSlot::slot), ItemStack.MAP_CODEC.forGetter(ItemStackWithSlot::stack)
         )
         .apply(p_406430_, ItemStackWithSlot::new)
   );

   public boolean isValidInContainer(int p_406351_) {
      return this.slot >= 0 && this.slot < p_406351_;
   }
}
