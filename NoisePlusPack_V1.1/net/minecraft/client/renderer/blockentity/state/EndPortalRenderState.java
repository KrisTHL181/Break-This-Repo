package net.minecraft.client.renderer.blockentity.state;

import java.util.EnumSet;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndPortalRenderState extends BlockEntityRenderState {
   public EnumSet<Direction> facesToShow = EnumSet.noneOf(Direction.class);
}
