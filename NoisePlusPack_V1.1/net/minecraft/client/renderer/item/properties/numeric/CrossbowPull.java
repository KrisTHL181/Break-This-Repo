package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CrossbowPull implements RangeSelectItemModelProperty {
   public static final MapCodec<CrossbowPull> MAP_CODEC = MapCodec.unit(new CrossbowPull());

   @Override
   public float get(ItemStack p_376874_, @Nullable ClientLevel p_377083_, @Nullable ItemOwner p_424014_, int p_377235_) {
      LivingEntity livingentity = p_424014_ == null ? null : p_424014_.asLivingEntity();
      if (livingentity == null) {
         return 0.0F;
      }

      if (CrossbowItem.isCharged(p_376874_)) {
         return 0.0F;
      }

      int i = CrossbowItem.getChargeDuration(p_376874_, livingentity);
      return (float)UseDuration.useDuration(p_376874_, livingentity) / i;
   }

   @Override
   public MapCodec<CrossbowPull> type() {
      return MAP_CODEC;
   }
}
