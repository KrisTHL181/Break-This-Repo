package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record FishingRodCast() implements ConditionalItemModelProperty {
   public static final MapCodec<FishingRodCast> MAP_CODEC = MapCodec.unit(new FishingRodCast());

   @Override
   public boolean get(ItemStack p_378314_, @Nullable ClientLevel p_376766_, @Nullable LivingEntity p_377545_, int p_375404_, ItemDisplayContext p_375458_) {
      if (p_377545_ instanceof Player player && player.fishing != null) {
         HumanoidArm humanoidarm = FishingHookRenderer.getHoldingArm(player);
         return p_377545_.getItemHeldByArm(humanoidarm) == p_378314_;
      } else {
         return false;
      }
   }

   @Override
   public MapCodec<FishingRodCast> type() {
      return MAP_CODEC;
   }
}
