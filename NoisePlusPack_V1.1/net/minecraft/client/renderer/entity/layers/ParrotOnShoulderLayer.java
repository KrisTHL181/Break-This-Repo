package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParrotOnShoulderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
   private final ParrotModel model;

   public ParrotOnShoulderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> p_174511_, EntityModelSet p_174512_) {
      super(p_174511_);
      this.model = new ParrotModel(p_174512_.bakeLayer(ModelLayers.PARROT));
   }

   public void submit(PoseStack p_428208_, SubmitNodeCollector p_423726_, int p_424313_, AvatarRenderState p_429711_, float p_425519_, float p_431641_) {
      Parrot.Variant parrot$variant = p_429711_.parrotOnLeftShoulder;
      if (parrot$variant != null) {
         this.submitOnShoulder(p_428208_, p_423726_, p_424313_, p_429711_, parrot$variant, p_425519_, p_431641_, true);
      }

      Parrot.Variant parrot$variant1 = p_429711_.parrotOnRightShoulder;
      if (parrot$variant1 != null) {
         this.submitOnShoulder(p_428208_, p_423726_, p_424313_, p_429711_, parrot$variant1, p_425519_, p_431641_, false);
      }
   }

   private void submitOnShoulder(
      PoseStack p_431021_,
      SubmitNodeCollector p_422981_,
      int p_427224_,
      AvatarRenderState p_425167_,
      Parrot.Variant p_452298_,
      float p_428398_,
      float p_428084_,
      boolean p_431155_
   ) {
      p_431021_.pushPose();
      p_431021_.translate(p_431155_ ? 0.4F : -0.4F, p_425167_.isCrouching ? -1.3F : -1.5F, 0.0F);
      ParrotRenderState parrotrenderstate = new ParrotRenderState();
      parrotrenderstate.pose = ParrotModel.Pose.ON_SHOULDER;
      parrotrenderstate.ageInTicks = p_425167_.ageInTicks;
      parrotrenderstate.walkAnimationPos = p_425167_.walkAnimationPos;
      parrotrenderstate.walkAnimationSpeed = p_425167_.walkAnimationSpeed;
      parrotrenderstate.yRot = p_428398_;
      parrotrenderstate.xRot = p_428084_;
      p_422981_.submitModel(
         this.model,
         parrotrenderstate,
         p_431021_,
         this.model.renderType(ParrotRenderer.getVariantTexture(p_452298_)),
         p_427224_,
         OverlayTexture.NO_OVERLAY,
         p_425167_.outlineColor,
         null
      );
      p_431021_.popPose();
   }
}
