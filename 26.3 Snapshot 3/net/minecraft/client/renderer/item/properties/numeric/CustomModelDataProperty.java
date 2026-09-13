package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jspecify.annotations.Nullable;

public record CustomModelDataProperty(int index) implements RangeSelectItemModelProperty {
   public static final MapCodec<CustomModelDataProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataProperty::index)).apply(i, CustomModelDataProperty::new)
   );

   @Override
   public float get(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable ItemOwner owner, final int seed) {
      CustomModelData customModelData = itemStack.get(DataComponents.CUSTOM_MODEL_DATA);
      if (customModelData != null) {
         Float value = customModelData.getFloat(this.index);
         if (value != null) {
            return value;
         }
      }

      return 0.0F;
   }

   @Override
   public MapCodec<CustomModelDataProperty> type() {
      return MAP_CODEC;
   }
}
