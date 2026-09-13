package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BrushableBlockRenderState extends BlockEntityRenderState {
   public ItemStackRenderState itemState = new ItemStackRenderState();
   public int dustProgress;
   public @Nullable Direction hitDirection;
}
