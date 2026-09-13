package net.minecraft.client.renderer.blockentity;

import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TheEndPortalRenderer extends AbstractEndPortalRenderer<TheEndPortalBlockEntity, EndPortalRenderState> {
   public EndPortalRenderState createRenderState() {
      return new EndPortalRenderState();
   }
}
