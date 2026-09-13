package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jspecify.annotations.Nullable;

public class ItemFrameRenderState extends EntityRenderState {
   public Direction direction = Direction.NORTH;
   public final BlockModelRenderState frameModel = new BlockModelRenderState();
   public final ItemStackRenderState item = new ItemStackRenderState();
   public int rotation;
   public boolean isGlowFrame;
   public @Nullable MapId mapId;
   public final MapRenderState mapRenderState = new MapRenderState();
}
