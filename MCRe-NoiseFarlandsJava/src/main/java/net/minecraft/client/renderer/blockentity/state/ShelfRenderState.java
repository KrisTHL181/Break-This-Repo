package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShelfRenderState extends BlockEntityRenderState {
    public final @Nullable ItemStackRenderState[] items = new ItemStackRenderState[3];
    public boolean alignToBottom;
    public Direction facing = Direction.NORTH;
}