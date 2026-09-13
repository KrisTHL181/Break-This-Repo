package net.minecraft.client.model.animal.fox;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.util.Mth;

public class AdultFoxModel extends FoxModel {
   private static final int LEG_SIZE = 6;
   private static final float HEAD_HEIGHT = 16.5F;
   private static final float LEG_POS = 17.5F;

   public AdultFoxModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(1, 5).addBox(-3.0F, -2.0F, -5.0F, 8.0F, 6.0F, 6.0F), PartPose.offset(-1.0F, 16.5F, -3.0F)
      );
      head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(8, 1).addBox(-3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
      head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(15, 1).addBox(3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
      head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(6, 18).addBox(-1.0F, 2.01F, -8.0F, 4.0F, 2.0F, 3.0F), PartPose.ZERO);
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(24, 15).addBox(-3.0F, 3.999F, -3.5F, 6.0F, 11.0F, 6.0F),
         PartPose.offsetAndRotation(0.0F, 16.0F, -6.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      CubeDeformation fudge = new CubeDeformation(0.001F);
      CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(4, 24).addBox(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, fudge);
      CubeListBuilder rightLeg = CubeListBuilder.create().texOffs(13, 24).addBox(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, fudge);
      root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-5.0F, 17.5F, 7.0F));
      root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(-1.0F, 17.5F, 7.0F));
      root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-5.0F, 17.5F, 0.0F));
      root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(-1.0F, 17.5F, 0.0F));
      body.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(30, 0).addBox(2.0F, 0.0F, -1.0F, 4.0F, 9.0F, 5.0F),
         PartPose.offsetAndRotation(-4.0F, 15.0F, -1.0F, -0.05235988F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 48, 32);
   }

   @Override
   protected void setSittingPose(final FoxRenderState state) {
      super.setSittingPose(state);
      this.body.xRot = (float) (Math.PI / 6);
      this.body.y -= 7.0F;
      this.body.z += 3.0F;
      this.tail.xRot = (float) (Math.PI / 4);
      this.head.y -= 6.5F;
      this.head.z += 2.75F;
      this.rightFrontLeg.xRot = (float) (-Math.PI / 12);
      this.leftFrontLeg.xRot = (float) (-Math.PI / 12);
      this.rightHindLeg.xRot = (float) (-Math.PI * 5.0 / 12.0);
      this.rightHindLeg.y += 4.0F;
      this.rightHindLeg.z -= 0.25F;
      this.leftHindLeg.xRot = (float) (-Math.PI * 5.0 / 12.0);
      this.leftHindLeg.y += 4.0F;
      this.leftHindLeg.z -= 0.25F;
      this.tail.z--;
   }

   @Override
   protected void setSleepingPose(final FoxRenderState state) {
      super.setSleepingPose(state);
      this.body.zRot = (float) (-Math.PI / 2);
      this.body.y += 5.0F;
      this.tail.xRot = (float) (-Math.PI * 5.0 / 6.0);
      this.head.x += 2.0F;
      this.head.y += 2.99F;
      this.head.yRot = (float) (-Math.PI * 2.0 / 3.0);
      this.head.zRot = 0.0F;
   }

   @Override
   protected void setWalkingPose(final FoxRenderState state) {
      super.setWalkingPose(state);
      float animationSpeed = state.walkAnimationSpeed;
      float animationPos = state.walkAnimationPos;
      this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
      this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F * animationSpeed;
      this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F * animationSpeed;
      this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
   }

   @Override
   protected void setCrouchingPose(final FoxRenderState state) {
      super.setCrouchingPose(state);
      this.body.y = this.body.y + state.crouchAmount;
   }

   @Override
   protected void setPouncingPose(final FoxRenderState state) {
      super.setPouncingPose(state);
      float crouch = state.crouchAmount / 2.0F;
      this.body.y -= crouch;
      this.head.y -= crouch;
   }
}
