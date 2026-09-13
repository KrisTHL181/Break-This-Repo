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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record CustomModelDataProperty(int index) implements SelectItemModelProperty<String> {
   public static final PrimitiveCodec<String> VALUE_CODEC = Codec.STRING;
   public static final SelectItemModelProperty.Type<CustomModelDataProperty, String> TYPE = SelectItemModelProperty.Type.create(
      RecordCodecBuilder.mapCodec(
         p_378187_ -> p_378187_.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataProperty::index))
            .apply(p_378187_, CustomModelDataProperty::new)
      ),
      VALUE_CODEC
   );

   public @Nullable String get(
      ItemStack p_378758_, @Nullable ClientLevel p_376603_, @Nullable LivingEntity p_375827_, int p_376216_, ItemDisplayContext p_378582_
   ) {
      CustomModelData custommodeldata = p_378758_.get(DataComponents.CUSTOM_MODEL_DATA);
      return custommodeldata != null ? custommodeldata.getString(this.index) : null;
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
