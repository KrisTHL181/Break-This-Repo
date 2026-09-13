package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.MinecartTntRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;

public class TntMinecartRenderer extends AbstractMinecartRenderer<MinecartTNT, MinecartTntRenderState> {
   public TntMinecartRenderer(final EntityRendererProvider.Context context) {
      super(context, ModelLayers.TNT_MINECART);
   }

   protected void submitMinecartContents(
      final MinecartTntRenderState state,
      final BlockModelRenderState blockModel,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords
   ) {
      float fuse = state.fuseRemainingInTicks;
      if (fuse > -1.0F && fuse < 10.0F) {
         float swell = TntRenderer.getSwellAmount(fuse);
         poseStack.translate(-swell * 0.5, 0.0, -swell * 0.5);
         float scale = 1.0F + swell;
         poseStack.scale(scale, scale, scale);
      }

      submitWhiteSolidBlock(blockModel, poseStack, submitNodeCollector, lightCoords, TntRenderer.isLit(fuse), state.outlineColor);
   }

   public static void submitWhiteSolidBlock(
      final BlockModelRenderState blockModel,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final boolean white,
      final int outlineColor
   ) {
      int overlayCoords;
      if (white) {
         overlayCoords = OverlayTexture.pack(OverlayTexture.u(1.0F), 10);
      } else {
         overlayCoords = OverlayTexture.NO_OVERLAY;
      }

      blockModel.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
   }

   public MinecartTntRenderState createRenderState() {
      return new MinecartTntRenderState();
   }

   public void extractRenderState(final MinecartTNT entity, final MinecartTntRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.fuseRemainingInTicks = entity.getFuse() > -1 ? entity.getFuse() - partialTicks + 1.0F : -1.0F;
   }
}
