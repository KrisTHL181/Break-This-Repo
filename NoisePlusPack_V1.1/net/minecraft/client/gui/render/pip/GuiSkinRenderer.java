package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiSkinRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fStack;

@OnlyIn(Dist.CLIENT)
public class GuiSkinRenderer extends PictureInPictureRenderer<GuiSkinRenderState> {
   public GuiSkinRenderer(MultiBufferSource.BufferSource p_406595_) {
      super(p_406595_);
   }

   @Override
   public Class<GuiSkinRenderState> getRenderStateClass() {
      return GuiSkinRenderState.class;
   }

   protected void renderToTexture(GuiSkinRenderState p_409195_, PoseStack p_409528_) {
      Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.PLAYER_SKIN);
      int i = Minecraft.getInstance().getWindow().getGuiScale();
      Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
      matrix4fstack.pushMatrix();
      float f = p_409195_.scale() * i;
      matrix4fstack.rotateAround(Axis.XP.rotationDegrees(p_409195_.rotationX()), 0.0F, f * -p_409195_.pivotY(), 0.0F);
      p_409528_.mulPose(Axis.YP.rotationDegrees(-p_409195_.rotationY()));
      p_409528_.translate(0.0F, -1.6010001F, 0.0F);
      RenderType rendertype = p_409195_.playerModel().renderType(p_409195_.texture());
      p_409195_.playerModel().renderToBuffer(p_409528_, this.bufferSource.getBuffer(rendertype), 15728880, OverlayTexture.NO_OVERLAY);
      this.bufferSource.endBatch();
      matrix4fstack.popMatrix();
   }

   @Override
   protected String getTextureLabel() {
      return "player skin";
   }
}
