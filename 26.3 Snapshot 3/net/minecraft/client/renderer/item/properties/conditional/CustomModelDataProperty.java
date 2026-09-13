package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jspecify.annotations.Nullable;

public record CustomModelDataProperty(int index) implements ConditionalItemModelProperty {
   public static final MapCodec<CustomModelDataProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataProperty::index)).apply(i, CustomModelDataProperty::new)
   );

   @Override
   public boolean get(
      final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner, final int seed, final ItemDisplayContext displayContext
   ) {
      CustomModelData customModelData = itemStack.get(DataComponents.CUSTOM_MODEL_DATA);
      return customModelData != null ? customModelData.getBoolean(this.index) == Boolean.TRUE : false;
   }

   @Override
   public MapCodec<CustomModelDataProperty> type() {
      return MAP_CODEC;
   }
}
