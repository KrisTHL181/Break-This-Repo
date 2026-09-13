package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BellRenderState extends BlockEntityRenderState {
    public @Nullable Direction shakeDirection;
    public float ticks;
}