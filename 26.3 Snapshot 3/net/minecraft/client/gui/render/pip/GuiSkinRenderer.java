package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.GuiSkinRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Unit;

public class GuiSkinRenderer extends PictureInPictureRenderer<GuiSkinRenderState> {
   @Override
   public Class<GuiSkinRenderState> getRenderStateClass() {
      return GuiSkinRenderState.class;
   }

   protected void renderToTexture(final GuiSkinRenderState skinState, final PoseStack modelStack, final SubmitNodeCollector submitNodeCollector) {
      Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.PLAYER_SKIN);
      int guiScale = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.guiScale;
      float scale = skinState.scale() * guiScale;
      RenderSystem.getModelViewStack().rotateAround(Axis.XP.rotationDegrees(skinState.rotationX()), 0.0F, scale * -skinState.pivotY(), 0.0F);
      modelStack.mulPose(Axis.YP.rotationDegrees(-skinState.rotationY()));
      modelStack.translate(0.0F, -1.6010001F, 0.0F);
      submitNodeCollector.submitModel(skinState.playerModel(), Unit.INSTANCE, modelStack, skinState.texture(), 15728880, OverlayTexture.NO_OVERLAY, 0);
   }

   @Override
   protected String getTextureLabel() {
      return "player skin";
   }
}
