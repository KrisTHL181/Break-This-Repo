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
public record IsSelected() implements ConditionalItemModelProperty {
   public static final MapCodec<IsSelected> MAP_CODEC = MapCodec.unit(new IsSelected());

   @Override
   public boolean get(ItemStack p_375683_, @Nullable ClientLevel p_376993_, @Nullable LivingEntity p_376986_, int p_377393_, ItemDisplayContext p_378469_) {
      return p_376986_ instanceof LocalPlayer localplayer && localplayer.getInventory().getSelectedItem() == p_375683_;
   }

   @Override
   public MapCodec<IsSelected> type() {
      return MAP_CODEC;
   }
}
