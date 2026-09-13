package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.GuiBookModelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class GuiBookModelRenderer extends PictureInPictureRenderer<GuiBookModelRenderState> {
   @Override
   public Class<GuiBookModelRenderState> getRenderStateClass() {
      return GuiBookModelRenderState.class;
   }

   protected void renderToTexture(final GuiBookModelRenderState bookModelState, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector) {
      Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(25.0F));
      float open = bookModelState.open();
      poseStack.translate((1.0F - open) * 0.2F, (1.0F - open) * 0.1F, (1.0F - open) * 0.25F);
      poseStack.mulPose(Axis.YP.rotationDegrees(-(1.0F - open) * 90.0F - 90.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
      float flip = bookModelState.flip();
      float pageFlip1 = Mth.clamp(Mth.frac(flip + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
      float pageFlip2 = Mth.clamp(Mth.frac(flip + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
      BookModel.State state = BookModel.State.forAnimation(0.0F, pageFlip1, pageFlip2, open);
      submitNodeCollector.submitModel(bookModelState.bookModel(), state, poseStack, bookModelState.texture(), 15728880, OverlayTexture.NO_OVERLAY, 0);
   }

   @Override
   protected float getTranslateY(final int height, final int guiScale) {
      return 17 * guiScale;
   }

   @Override
   protected String getTextureLabel() {
      return "book model";
   }
}
