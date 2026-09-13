package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCowRenderState extends LivingEntityRenderState {
    public MushroomCow.Variant variant = MushroomCow.Variant.RED;
    public final BlockModelRenderState mushroomModel = new BlockModelRenderState();
}