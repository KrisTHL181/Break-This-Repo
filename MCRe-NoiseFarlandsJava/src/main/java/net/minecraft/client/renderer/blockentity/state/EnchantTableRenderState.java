package net.minecraft.client.renderer.blockentity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnchantTableRenderState extends BlockEntityRenderState {
    public float time;
    public float yRot;
    public float flip;
    public float open;
}