package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record IsUsingItem() implements ConditionalItemModelProperty {
   public static final MapCodec<IsUsingItem> MAP_CODEC = MapCodec.unit(new IsUsingItem());

   @Override
   public boolean get(ItemStack p_375989_, @Nullable ClientLevel p_378132_, @Nullable LivingEntity p_376396_, int p_376260_, ItemDisplayContext p_377905_) {
      return p_376396_ == null ? false : p_376396_.isUsingItem() && p_376396_.getUseItem() == p_375989_;
   }

   @Override
   public MapCodec<IsUsingItem> type() {
      return MAP_CODEC;
   }
}
