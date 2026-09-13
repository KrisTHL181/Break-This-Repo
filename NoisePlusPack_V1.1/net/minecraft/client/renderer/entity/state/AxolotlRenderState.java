package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AxolotlRenderState extends LivingEntityRenderState {
   public Axolotl.Variant variant = Axolotl.Variant.DEFAULT;
   public float playingDeadFactor;
   public float movingFactor;
   public float inWaterFactor = 1.0F;
   public float onGroundFactor;
}
