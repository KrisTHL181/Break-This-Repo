package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RabbitRenderState extends LivingEntityRenderState {
   public float jumpCompletion;
   public boolean isToast;
   public Rabbit.Variant variant = Rabbit.Variant.DEFAULT;
}
