package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShelfRenderState extends BlockEntityRenderState {
   public @Nullable ItemStackRenderState[] items = new ItemStackRenderState[3];
   public boolean alignToBottom;
}
