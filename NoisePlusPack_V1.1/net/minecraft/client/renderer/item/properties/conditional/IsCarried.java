package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record IsCarried() implements ConditionalItemModelProperty {
   public static final MapCodec<IsCarried> MAP_CODEC = MapCodec.unit(new IsCarried());

   @Override
   public boolean get(ItemStack p_377567_, @Nullable ClientLevel p_376720_, @Nullable LivingEntity p_376604_, int p_375610_, ItemDisplayContext p_375638_) {
      return p_376604_ instanceof LocalPlayer localplayer && localplayer.containerMenu.getCarried() == p_377567_;
   }

   @Override
   public MapCodec<IsCarried> type() {
      return MAP_CODEC;
   }
}
