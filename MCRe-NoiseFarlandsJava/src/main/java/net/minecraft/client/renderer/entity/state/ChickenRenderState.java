package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChickenRenderState extends LivingEntityRenderState {
    public float flap;
    public float flapSpeed;
    public @Nullable ChickenVariant variant;
}