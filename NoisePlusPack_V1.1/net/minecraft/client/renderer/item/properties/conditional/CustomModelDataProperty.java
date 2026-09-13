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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record CustomModelDataProperty(int index) implements ConditionalItemModelProperty {
   public static final MapCodec<CustomModelDataProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_378536_ -> p_378536_.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataProperty::index))
         .apply(p_378536_, CustomModelDataProperty::new)
   );

   @Override
   public boolean get(ItemStack p_378321_, @Nullable ClientLevel p_375707_, @Nullable LivingEntity p_375621_, int p_377539_, ItemDisplayContext p_378822_) {
      CustomModelData custommodeldata = p_378321_.get(DataComponents.CUSTOM_MODEL_DATA);
      return custommodeldata != null ? custommodeldata.getBoolean(this.index) == Boolean.TRUE : false;
   }

   @Override
   public MapCodec<CustomModelDataProperty> type() {
      return MAP_CODEC;
   }
}
