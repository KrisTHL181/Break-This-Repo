package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ArrowRenderer<T extends AbstractArrow, S extends ArrowRenderState> extends EntityRenderer<T, S> {
   private final ArrowModel model;

   public ArrowRenderer(EntityRendererProvider.Context p_173917_) {
      super(p_173917_);
      this.model = new ArrowModel(p_173917_.bakeLayer(ModelLayers.ARROW));
   }

   public void submit(S p_424866_, PoseStack p_431358_, SubmitNodeCollector p_428136_, CameraRenderState p_431633_) {
      p_431358_.pushPose();
      p_431358_.mulPose(Axis.YP.rotationDegrees(p_424866_.yRot - 90.0F));
      p_431358_.mulPose(Axis.ZP.rotationDegrees(p_424866_.xRot));
      p_428136_.submitModel(
         this.model,
         p_424866_,
         p_431358_,
         RenderTypes.entityCutout(this.getTextureLocation(p_424866_)),
         p_424866_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         p_424866_.outlineColor,
         null
      );
      p_431358_.popPose();
      super.submit(p_424866_, p_431358_, p_428136_, p_431633_);
   }

   protected abstract Identifier getTextureLocation(S var1);

   public void extractRenderState(T p_455901_, S p_367796_, float p_365866_) {
      super.extractRenderState(p_455901_, p_367796_, p_365866_);
      p_367796_.xRot = p_455901_.getXRot(p_365866_);
      p_367796_.yRot = p_455901_.getYRot(p_365866_);
      p_367796_.shake = p_455901_.shakeTime - p_365866_;
   }
}
