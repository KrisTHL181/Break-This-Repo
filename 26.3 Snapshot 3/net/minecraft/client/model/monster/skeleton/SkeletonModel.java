package net.minecraft.client.model.monster.skeleton;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class SkeletonModel<S extends SkeletonRenderState> extends HumanoidModel<S> {
   public SkeletonModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
      PartDefinition root = mesh.getRoot();
      createDefaultSkeletonMesh(root);
      return LayerDefinition.create(mesh, 64, 32);
   }

   protected static void createDefaultSkeletonMesh(final PartDefinition root) {
      root.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-5.0F, 2.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(5.0F, 2.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(2.0F, 12.0F, 0.0F)
      );
   }

   public static LayerDefinition createSingleModelDualBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition root = meshdefinition.getRoot();
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(16, 16)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
            .texOffs(28, 0)
            .addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F)
            .texOffs(16, 48)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.025F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
               .texOffs(0, 0)
               .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
               .texOffs(0, 32)
               .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.2F)),
            PartPose.offset(0.0F, 0.0F, 0.0F)
         )
         .addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
      root.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create()
            .texOffs(40, 16)
            .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
            .texOffs(42, 33)
            .addBox(-1.55F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F),
         PartPose.offset(-5.5F, 2.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create()
            .texOffs(56, 16)
            .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
            .texOffs(40, 48)
            .addBox(-1.45F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F),
         PartPose.offset(5.5F, 2.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F).texOffs(0, 49).addBox(-1.5F, -0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
         PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F).texOffs(4, 49).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
         PartPose.offset(2.0F, 12.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(final S state) {
      super.setupAnim(state);
      if (state.isAggressive && !state.isHoldingBow) {
         float attackTime = state.attackTime;
         float attack2 = Mth.sin(attackTime * (float) Math.PI);
         float attack = Mth.sin((1.0F - (1.0F - attackTime) * (1.0F - attackTime)) * (float) Math.PI);
         this.rightArm.zRot = 0.0F;
         this.leftArm.zRot = 0.0F;
         this.rightArm.yRot = -(0.1F - attack2 * 0.6F);
         this.leftArm.yRot = 0.1F - attack2 * 0.6F;
         this.rightArm.xRot = (float) (-Math.PI / 2);
         this.leftArm.xRot = (float) (-Math.PI / 2);
         this.rightArm.xRot -= attack2 * 1.2F - attack * 0.4F;
         this.leftArm.xRot -= attack2 * 1.2F - attack * 0.4F;
         AnimationUtils.bobArms(this.rightArm, this.leftArm, state.ageInTicks);
      }
   }

   public void translateToHand(final SkeletonRenderState state, final HumanoidArm arm, final PoseStack poseStack) {
      this.root().translateAndRotate(poseStack);
      float offset = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      ModelPart part = this.getArm(arm);
      part.x += offset;
      part.translateAndRotate(poseStack);
      part.x -= offset;
   }
}
