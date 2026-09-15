package net.minecraft.client.renderer.blockentity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LecternRenderState extends BlockEntityRenderState {
    public boolean hasBook;
    public float yRot;
}