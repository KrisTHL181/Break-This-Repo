package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BundleFullness() implements RangeSelectItemModelProperty {
   public static final MapCodec<BundleFullness> MAP_CODEC = MapCodec.unit(new BundleFullness());

   @Override
   public float get(ItemStack p_375568_, @Nullable ClientLevel p_375750_, @Nullable ItemOwner p_426304_, int p_376397_) {
      return BundleItem.getFullnessDisplay(p_375568_);
   }

   @Override
   public MapCodec<BundleFullness> type() {
      return MAP_CODEC;
   }
}
