package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HorseRenderState extends EquineRenderState {
    public Variant variant = Variant.WHITE;
    public Markings markings = Markings.NONE;
}