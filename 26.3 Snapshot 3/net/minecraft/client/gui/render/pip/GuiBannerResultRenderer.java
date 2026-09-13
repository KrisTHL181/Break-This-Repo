package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.state.gui.pip.GuiBannerResultRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;

public class GuiBannerResultRenderer extends PictureInPictureRenderer<GuiBannerResultRenderState> {
   private final SpriteGetter sprites;

   public GuiBannerResultRenderer(final SpriteGetter sprites) {
      this.sprites = sprites;
   }

   @Override
   public Class<GuiBannerResultRenderState> getRenderStateClass() {
      return GuiBannerResultRenderState.class;
   }

   protected void renderToTexture(final GuiBannerResultRenderState renderState, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector) {
      Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
      poseStack.translate(0.0F, 0.25F, 0.0F);
      submitNodeCollector.submitModel(renderState.flag(), 0.0F, poseStack, 15728880, OverlayTexture.NO_OVERLAY, -1, Sheets.BANNER_BASE, this.sprites, 0);
      BannerRenderer.submitPatterns(
         this.sprites,
         poseStack,
         submitNodeCollector,
         15728880,
         OverlayTexture.NO_OVERLAY,
         renderState.flag(),
         0.0F,
         true,
         renderState.baseColor(),
         renderState.resultBannerPatterns()
      );
   }

   @Override
   protected String getTextureLabel() {
      return "banner result";
   }
}
