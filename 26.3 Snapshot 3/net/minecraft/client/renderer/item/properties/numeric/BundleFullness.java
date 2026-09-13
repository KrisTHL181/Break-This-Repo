package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record BundleFullness() implements RangeSelectItemModelProperty {
   public static final MapCodec<BundleFullness> MAP_CODEC = MapCodec.unit(new BundleFullness());

   @Override
   public float get(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable ItemOwner owner, final int seed) {
      return BundleItem.getFullnessDisplay(itemStack);
   }

   @Override
   public MapCodec<BundleFullness> type() {
      return MAP_CODEC;
   }
}
