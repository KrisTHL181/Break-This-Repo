package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class GuiEntityRenderer extends PictureInPictureRenderer<GuiEntityRenderState> {
   private final EntityRenderDispatcher entityRenderDispatcher;

   public GuiEntityRenderer(MultiBufferSource.BufferSource p_406505_, EntityRenderDispatcher p_409598_) {
      super(p_406505_);
      this.entityRenderDispatcher = p_409598_;
   }

   @Override
   public Class<GuiEntityRenderState> getRenderStateClass() {
      return GuiEntityRenderState.class;
   }

   protected void renderToTexture(GuiEntityRenderState p_408559_, PoseStack p_410540_) {
      Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
      Vector3f vector3f = p_408559_.translation();
      p_410540_.translate(vector3f.x, vector3f.y, vector3f.z);
      p_410540_.mulPose(p_408559_.rotation());
      Quaternionf quaternionf = p_408559_.overrideCameraAngle();
      FeatureRenderDispatcher featurerenderdispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
      CameraRenderState camerarenderstate = new CameraRenderState();
      if (quaternionf != null) {
         camerarenderstate.orientation = quaternionf.conjugate(new Quaternionf()).rotateY((float) Math.PI);
      }

      this.entityRenderDispatcher.submit(p_408559_.renderState(), camerarenderstate, 0.0, 0.0, 0.0, p_410540_, featurerenderdispatcher.getSubmitNodeStorage());
      featurerenderdispatcher.renderAllFeatures();
   }

   @Override
   protected float getTranslateY(int p_409319_, int p_407944_) {
      return p_409319_ / 2.0F;
   }

   @Override
   protected String getTextureLabel() {
      return "entity";
   }
}
