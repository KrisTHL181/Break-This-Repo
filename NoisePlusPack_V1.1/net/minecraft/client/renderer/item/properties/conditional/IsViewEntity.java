package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record IsViewEntity() implements ConditionalItemModelProperty {
   public static final MapCodec<IsViewEntity> MAP_CODEC = MapCodec.unit(new IsViewEntity());

   @Override
   public boolean get(ItemStack p_378008_, @Nullable ClientLevel p_378463_, @Nullable LivingEntity p_376951_, int p_376819_, ItemDisplayContext p_377915_) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      return entity != null ? p_376951_ == entity : p_376951_ == minecraft.player;
   }

   @Override
   public MapCodec<IsViewEntity> type() {
      return MAP_CODEC;
   }
}
