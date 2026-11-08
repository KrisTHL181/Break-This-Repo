package net.minecraft.client.renderer.blockentity.state;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class CampfireRenderState extends BlockEntityRenderState {
   public List<ItemStackRenderState> items = Collections.emptyList();
   public Direction facing = Direction.NORTH;
}
