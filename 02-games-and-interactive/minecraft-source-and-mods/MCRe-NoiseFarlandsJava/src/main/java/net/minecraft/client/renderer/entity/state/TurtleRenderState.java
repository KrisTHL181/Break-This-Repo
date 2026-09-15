package net.minecraft.client.renderer.entity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TurtleRenderState extends LivingEntityRenderState {
    public boolean isOnLand;
    public boolean isLayingEgg;
    public boolean hasEgg;
}