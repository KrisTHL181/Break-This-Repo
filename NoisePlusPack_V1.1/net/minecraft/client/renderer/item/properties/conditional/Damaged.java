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
public record Damaged() implements ConditionalItemModelProperty {
   public static final MapCodec<Damaged> MAP_CODEC = MapCodec.unit(new Damaged());

   @Override
   public boolean get(ItemStack p_378726_, @Nullable ClientLevel p_378780_, @Nullable LivingEntity p_376626_, int p_376393_, ItemDisplayContext p_377268_) {
      return p_378726_.isDamaged();
   }

   @Override
   public MapCodec<Damaged> type() {
      return MAP_CODEC;
   }
}
