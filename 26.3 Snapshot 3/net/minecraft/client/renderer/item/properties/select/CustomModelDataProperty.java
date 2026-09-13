package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jspecify.annotations.Nullable;

public record CustomModelDataProperty(int index) implements SelectItemModelProperty<String> {
   public static final PrimitiveCodec<String> VALUE_CODEC = Codec.STRING;
   public static final SelectItemModelProperty.Type<CustomModelDataProperty, String> TYPE = SelectItemModelProperty.Type.create(
      RecordCodecBuilder.mapCodec(
         i -> i.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataProperty::index))
            .apply(i, CustomModelDataProperty::new)
      ),
      VALUE_CODEC
   );

   public @Nullable String get(
      final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner, final int seed, final ItemDisplayContext displayContext
   ) {
      CustomModelData customModelData = itemStack.get(DataComponents.CUSTOM_MODEL_DATA);
      return customModelData != null ? customModelData.getString(this.index) : null;
   }

   @Override
   public SelectItemModelProperty.Type<CustomModelDataProperty, String> type() {
      return TYPE;
   }

   @Override
   public Codec<String> valueCodec() {
      return VALUE_CODEC;
   }
}
