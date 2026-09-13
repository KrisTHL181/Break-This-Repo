package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PanoramaRenderer {
   public static final Identifier PANORAMA_OVERLAY = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png");
   private final Minecraft minecraft;
   private final CubeMap cubeMap;
   private float spin;

   public PanoramaRenderer(CubeMap p_110002_) {
      this.cubeMap = p_110002_;
      this.minecraft = Minecraft.getInstance();
   }

   public void render(GuiGraphics p_331913_, int p_332706_, int p_333201_, boolean p_409164_) {
      if (p_409164_) {
         float f = this.minecraft.getDeltaTracker().getRealtimeDeltaTicks();
         float f1 = (float)(f * this.minecraft.options.panoramaSpeed().get());
         this.spin = wrap(this.spin + f1 * 0.1F, 360.0F);
      }

      this.cubeMap.render(this.minecraft, 10.0F, -this.spin);
      p_331913_.blit(RenderPipelines.GUI_TEXTURED, PANORAMA_OVERLAY, 0, 0, 0.0F, 0.0F, p_332706_, p_333201_, 16, 128, 16, 128);
   }

   private static float wrap(float p_249058_, float p_249548_) {
      return p_249058_ > p_249548_ ? p_249058_ - p_249548_ : p_249058_;
   }

   public void registerTextures(TextureManager p_408363_) {
      this.cubeMap.registerTextures(p_408363_);
   }
}
