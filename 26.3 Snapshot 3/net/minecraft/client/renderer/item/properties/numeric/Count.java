package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record Count(boolean normalize) implements RangeSelectItemModelProperty {
   public static final MapCodec<Count> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.BOOL.optionalFieldOf("normalize", true).forGetter(Count::normalize)).apply(i, Count::new)
   );

   @Override
   public float get(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable ItemOwner owner, final int seed) {
      float count = itemStack.getCount();
      float maxCount = itemStack.getMaxStackSize();
      return this.normalize ? Mth.clamp(count / maxCount, 0.0F, 1.0F) : Mth.clamp(count, 0.0F, maxCount);
   }

   @Override
   public MapCodec<Count> type() {
      return MAP_CODEC;
   }
}
