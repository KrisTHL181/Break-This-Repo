package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BannerRenderState extends BlockEntityRenderState {
   public DyeColor baseColor;
   public BannerPatternLayers patterns;
   public float phase;
   public float angle;
   public boolean standing;
}
