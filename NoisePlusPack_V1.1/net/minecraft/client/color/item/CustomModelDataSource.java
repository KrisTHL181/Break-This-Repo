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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record CustomModelDataSource(int index, int defaultColor) implements ItemTintSource {
   public static final MapCodec<CustomModelDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_377392_ -> p_377392_.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataSource::index),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(CustomModelDataSource::defaultColor)
         )
         .apply(p_377392_, CustomModelDataSource::new)
   );

   @Override
   public int calculate(ItemStack p_376452_, @Nullable ClientLevel p_376359_, @Nullable LivingEntity p_377042_) {
      CustomModelData custommodeldata = p_376452_.get(DataComponents.CUSTOM_MODEL_DATA);
      if (custommodeldata != null) {
         Integer integer = custommodeldata.getColor(this.index);
         if (integer != null) {
            return ARGB.opaque(integer);
         }
      }

      return ARGB.opaque(this.defaultColor);
   }

   @Override
   public MapCodec<CustomModelDataSource> type() {
      return MAP_CODEC;
   }
}
