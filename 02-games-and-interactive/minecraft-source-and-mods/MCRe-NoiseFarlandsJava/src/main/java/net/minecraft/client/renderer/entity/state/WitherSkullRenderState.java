package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherSkullRenderState extends EntityRenderState {
    public boolean isDangerous;
    public final SkullModelBase.State modelState = new SkullModelBase.State();
}