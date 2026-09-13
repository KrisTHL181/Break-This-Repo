package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jspecify.annotations.Nullable;

public record CustomModelDataSource(int index, int defaultColor) implements ItemTintSource {
   public static final MapCodec<CustomModelDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataSource::index),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(CustomModelDataSource::defaultColor)
         )
         .apply(i, CustomModelDataSource::new)
   );

   @Override
   public int calculate(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner) {
      CustomModelData customModelData = itemStack.get(DataComponents.CUSTOM_MODEL_DATA);
      if (customModelData != null) {
         Integer value = customModelData.getColor(this.index);
         if (value != null) {
            return ARGB.opaque(value);
         }
      }

      return ARGB.opaque(this.defaultColor);
   }

   @Override
   public MapCodec<CustomModelDataSource> type() {
      return MAP_CODEC;
   }
}
