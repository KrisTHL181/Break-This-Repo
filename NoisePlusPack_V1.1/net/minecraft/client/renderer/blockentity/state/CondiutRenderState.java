package net.minecraft.client.renderer.blockentity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CondiutRenderState extends BlockEntityRenderState {
   public float animTime;
   public boolean isActive;
   public float activeRotation;
   public int animationPhase;
   public boolean isHunting;
}
