package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.leash.LeashKnotModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LeashKnotRenderer extends EntityRenderer<LeashFenceKnotEntity, EntityRenderState> {
   private static final Identifier KNOT_LOCATION = Identifier.withDefaultNamespace("textures/entity/lead_knot.png");
   private final LeashKnotModel model;

   public LeashKnotRenderer(EntityRendererProvider.Context p_174284_) {
      super(p_174284_);
      this.model = new LeashKnotModel(p_174284_.bakeLayer(ModelLayers.LEASH_KNOT));
   }

   @Override
   public void submit(EntityRenderState p_430499_, PoseStack p_424161_, SubmitNodeCollector p_423381_, CameraRenderState p_427508_) {
      p_424161_.pushPose();
      p_424161_.scale(-1.0F, -1.0F, 1.0F);
      p_423381_.submitModel(
         this.model, p_430499_, p_424161_, this.model.renderType(KNOT_LOCATION), p_430499_.lightCoords, OverlayTexture.NO_OVERLAY, p_430499_.outlineColor, null
      );
      p_424161_.popPose();
      super.submit(p_430499_, p_424161_, p_423381_, p_427508_);
   }

   @Override
   public EntityRenderState createRenderState() {
      return new EntityRenderState();
   }
}
