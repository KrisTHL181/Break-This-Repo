package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BoatRenderer extends AbstractBoatRenderer {
   private final Model.Simple waterPatchModel;
   private final Identifier texture;
   private final EntityModel<BoatRenderState> model;

   public BoatRenderer(EntityRendererProvider.Context p_234563_, ModelLayerLocation p_369070_) {
      super(p_234563_);
      this.texture = p_369070_.model().withPath(p_357968_ -> "textures/entity/" + p_357968_ + ".png");
      this.waterPatchModel = new Model.Simple(p_234563_.bakeLayer(ModelLayers.BOAT_WATER_PATCH), p_448313_ -> RenderTypes.waterMask());
      this.model = new BoatModel(p_234563_.bakeLayer(p_369070_));
   }

   @Override
   protected EntityModel<BoatRenderState> model() {
      return this.model;
   }

   @Override
   protected RenderType renderType() {
      return this.model.renderType(this.texture);
   }

   @Override
   protected void submitTypeAdditions(BoatRenderState p_423463_, PoseStack p_422289_, SubmitNodeCollector p_427266_, int p_430134_) {
      if (!p_423463_.isUnderWater) {
         p_427266_.submitModel(
            this.waterPatchModel,
            Unit.INSTANCE,
            p_422289_,
            this.waterPatchModel.renderType(this.texture),
            p_430134_,
            OverlayTexture.NO_OVERLAY,
            p_423463_.outlineColor,
            null
         );
      }
   }
}
