package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiBookModelRenderState;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiBookModelRenderer extends PictureInPictureRenderer<GuiBookModelRenderState> {
   public GuiBookModelRenderer(MultiBufferSource.BufferSource p_407792_) {
      super(p_407792_);
   }

   @Override
   public Class<GuiBookModelRenderState> getRenderStateClass() {
      return GuiBookModelRenderState.class;
   }

   protected void renderToTexture(GuiBookModelRenderState p_410059_, PoseStack p_407927_) {
      Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
      p_407927_.mulPose(Axis.YP.rotationDegrees(180.0F));
      p_407927_.mulPose(Axis.XP.rotationDegrees(25.0F));
      float f = p_410059_.open();
      p_407927_.translate((1.0F - f) * 0.2F, (1.0F - f) * 0.1F, (1.0F - f) * 0.25F);
      p_407927_.mulPose(Axis.YP.rotationDegrees(-(1.0F - f) * 90.0F - 90.0F));
      p_407927_.mulPose(Axis.XP.rotationDegrees(180.0F));
      float f1 = p_410059_.flip();
      float f2 = Mth.clamp(Mth.frac(f1 + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
      float f3 = Mth.clamp(Mth.frac(f1 + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
      BookModel bookmodel = p_410059_.bookModel();
      bookmodel.setupAnim(new BookModel.State(0.0F, f2, f3, f));
      Identifier identifier = p_410059_.texture();
      VertexConsumer vertexconsumer = this.bufferSource.getBuffer(bookmodel.renderType(identifier));
      bookmodel.renderToBuffer(p_407927_, vertexconsumer, 15728880, OverlayTexture.NO_OVERLAY);
   }

   @Override
   protected float getTranslateY(int p_410523_, int p_407205_) {
      return 17 * p_407205_;
   }

   @Override
   protected String getTextureLabel() {
      return "book model";
   }
}
