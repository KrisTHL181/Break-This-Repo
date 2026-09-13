package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.PanoramaRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class Panorama {
   public static final Identifier PANORAMA_OVERLAY = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png");
   private float spin;
   private boolean shouldSpin = true;

   public void startSpin() {
      this.shouldSpin = true;
   }

   public void holdSpin() {
      this.shouldSpin = false;
   }

   public void extractRenderState(final GuiGraphicsExtractor graphics, final int width, final int height) {
      Minecraft minecraft = Minecraft.getInstance();
      if (this.shouldSpin) {
         float a = minecraft.getDeltaTracker().getRealtimeDeltaTicks();
         float delta = (float)(a * minecraft.gameRenderer.gameRenderState().optionsRenderState.panoramaSpeed);
         this.spin = Mth.wrapDegrees(this.spin + delta * 0.1F);
      }

      minecraft.gameRenderer.gameRenderState().guiRenderState.panoramaRenderState = new PanoramaRenderState(-this.spin);
      graphics.blit(RenderPipelines.GUI_TEXTURED, PANORAMA_OVERLAY, 0, 0, 0.0F, 0.0F, width, height, 16, 128, 16, 128);
   }
}
