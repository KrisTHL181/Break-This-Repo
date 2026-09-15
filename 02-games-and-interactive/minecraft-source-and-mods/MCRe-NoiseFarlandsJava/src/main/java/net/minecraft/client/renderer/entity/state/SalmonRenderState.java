package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SalmonRenderState extends LivingEntityRenderState {
    public Salmon.Variant variant = Salmon.Variant.MEDIUM;
}