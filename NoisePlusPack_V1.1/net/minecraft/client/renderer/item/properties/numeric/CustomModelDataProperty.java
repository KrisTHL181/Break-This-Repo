package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record CustomModelDataProperty(int index) implements RangeSelectItemModelProperty {
   public static final MapCodec<CustomModelDataProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_378172_ -> p_378172_.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataProperty::index))
         .apply(p_378172_, CustomModelDataProperty::new)
   );

   @Override
   public float get(ItemStack p_376860_, @Nullable ClientLevel p_378092_, @Nullable ItemOwner p_427630_, int p_376291_) {
      CustomModelData custommodeldata = p_376860_.get(DataComponents.CUSTOM_MODEL_DATA);
      if (custommodeldata != null) {
         Float f = custommodeldata.getFloat(this.index);
         if (f != null) {
            return f;
         }
      }

      return 0.0F;
   }

   @Override
   public MapCodec<CustomModelDataProperty> type() {
      return MAP_CODEC;
   }
}
