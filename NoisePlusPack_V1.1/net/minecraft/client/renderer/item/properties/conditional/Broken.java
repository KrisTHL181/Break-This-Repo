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
public record Broken() implements ConditionalItemModelProperty {
   public static final MapCodec<Broken> MAP_CODEC = MapCodec.unit(new Broken());

   @Override
   public boolean get(ItemStack p_376009_, @Nullable ClientLevel p_378275_, @Nullable LivingEntity p_375652_, int p_377220_, ItemDisplayContext p_376896_) {
      return p_376009_.nextDamageWillBreak();
   }

   @Override
   public MapCodec<Broken> type() {
      return MAP_CODEC;
   }
}
