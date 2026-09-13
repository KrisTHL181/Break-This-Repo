package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record Count(boolean normalize) implements RangeSelectItemModelProperty {
   public static final MapCodec<Count> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_377483_ -> p_377483_.group(Codec.BOOL.optionalFieldOf("normalize", true).forGetter(Count::normalize)).apply(p_377483_, Count::new)
   );

   @Override
   public float get(ItemStack p_376945_, @Nullable ClientLevel p_378670_, @Nullable ItemOwner p_425612_, int p_378604_) {
      float f = p_376945_.getCount();
      float f1 = p_376945_.getMaxStackSize();
      return this.normalize ? Mth.clamp(f / f1, 0.0F, 1.0F) : Mth.clamp(f, 0.0F, f1);
   }

   @Override
   public MapCodec<Count> type() {
      return MAP_CODEC;
   }
}
