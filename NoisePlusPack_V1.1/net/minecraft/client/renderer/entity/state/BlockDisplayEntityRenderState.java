package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.Display;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockDisplayEntityRenderState extends DisplayEntityRenderState {
   public Display.BlockDisplay.@Nullable BlockRenderState blockRenderState;

   @Override
   public boolean hasSubState() {
      return this.blockRenderState != null;
   }
}
