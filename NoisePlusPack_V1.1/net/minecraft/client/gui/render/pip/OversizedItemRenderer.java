package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OversizedItemRenderer extends PictureInPictureRenderer<OversizedItemRenderState> {
   private boolean usedOnThisFrame;
   private @Nullable Object modelOnTextureIdentity;

   public OversizedItemRenderer(MultiBufferSource.BufferSource p_409932_) {
      super(p_409932_);
   }

   public boolean usedOnThisFrame() {
      return this.usedOnThisFrame;
   }

   public void resetUsedOnThisFrame() {
      this.usedOnThisFrame = false;
   }

   public void invalidateTexture() {
      this.modelOnTextureIdentity = null;
   }

   @Override
   public Class<OversizedItemRenderState> getRenderStateClass() {
      return OversizedItemRenderState.class;
   }

   protected void renderToTexture(OversizedItemRenderState p_408881_, PoseStack p_405965_) {
      p_405965_.scale(1.0F, -1.0F, -1.0F);
      GuiItemRenderState guiitemrenderstate = p_408881_.guiItemRenderState();
      ScreenRectangle screenrectangle = guiitemrenderstate.oversizedItemBounds();
      Objects.requireNonNull(screenrectangle);
      float f = (screenrectangle.left() + screenrectangle.right()) / 2.0F;
      float f1 = (screenrectangle.top() + screenrectangle.bottom()) / 2.0F;
      float f2 = guiitemrenderstate.x() + 8.0F;
      float f3 = guiitemrenderstate.y() + 8.0F;
      p_405965_.translate((f2 - f) / 16.0F, (f1 - f3) / 16.0F, 0.0F);
      TrackingItemStackRenderState trackingitemstackrenderstate = guiitemrenderstate.itemStackRenderState();
      boolean flag = !trackingitemstackrenderstate.usesBlockLight();
      if (flag) {
         Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
      } else {
         Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
      }

      FeatureRenderDispatcher featurerenderdispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
      SubmitNodeStorage submitnodestorage = featurerenderdispatcher.getSubmitNodeStorage();
      trackingitemstackrenderstate.submit(p_405965_, submitnodestorage, 15728880, OverlayTexture.NO_OVERLAY, 0);
      featurerenderdispatcher.renderAllFeatures();
      this.modelOnTextureIdentity = trackingitemstackrenderstate.getModelIdentity();
   }

   public void blitTexture(OversizedItemRenderState p_407060_, GuiRenderState p_410164_) {
      super.blitTexture(p_407060_, p_410164_);
      this.usedOnThisFrame = true;
   }

   public boolean textureIsReadyToBlit(OversizedItemRenderState p_409767_) {
      TrackingItemStackRenderState trackingitemstackrenderstate = p_409767_.guiItemRenderState().itemStackRenderState();
      return !trackingitemstackrenderstate.isAnimated() && trackingitemstackrenderstate.getModelIdentity().equals(this.modelOnTextureIdentity);
   }

   @Override
   protected float getTranslateY(int p_410716_, int p_408311_) {
      return p_410716_ / 2.0F;
   }

   @Override
   protected String getTextureLabel() {
      return "oversized_item";
   }
}
