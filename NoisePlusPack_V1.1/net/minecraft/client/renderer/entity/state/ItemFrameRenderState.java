package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ItemFrameRenderState extends EntityRenderState {
   public Direction direction = Direction.NORTH;
   public final ItemStackRenderState item = new ItemStackRenderState();
   public int rotation;
   public boolean isGlowFrame;
   public @Nullable MapId mapId;
   public final MapRenderState mapRenderState = new MapRenderState();
}
