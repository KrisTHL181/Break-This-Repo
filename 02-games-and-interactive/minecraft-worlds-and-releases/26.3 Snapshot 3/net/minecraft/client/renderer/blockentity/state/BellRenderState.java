package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public class BellRenderState extends BlockEntityRenderState {
   public @Nullable Direction shakeDirection;
   public float ticks;
}
