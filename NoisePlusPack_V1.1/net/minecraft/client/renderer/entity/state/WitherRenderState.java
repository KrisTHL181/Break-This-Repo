package net.minecraft.client.renderer.entity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherRenderState extends LivingEntityRenderState {
   public float[] xHeadRots = new float[2];
   public float[] yHeadRots = new float[2];
   public float invulnerableTicks;
   public boolean isPowered;
}
