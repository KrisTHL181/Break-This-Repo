package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public class BrushableBlockRenderState extends BlockEntityRenderState {
   public final ItemStackRenderState itemState = new ItemStackRenderState();
   public int dustProgress;
   public @Nullable Direction hitDirection;
}
