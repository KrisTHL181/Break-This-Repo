package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.Crackiness;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IronGolemRenderState extends LivingEntityRenderState {
   public float attackTicksRemaining;
   public int offerFlowerTick;
   public Crackiness.Level crackiness = Crackiness.Level.NONE;
}
