package net.minecraft.client.model.animal.golem;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.util.Mth;

public class IronGolemModel extends EntityModel<IronGolemRenderState> {
   private final ModelPart head;
   private final ModelPart rightArm;
   private final ModelPart leftArm;
   private final ModelPart rightLeg;
   private final ModelPart leftLeg;

   public IronGolemModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");
      this.rightArm = root.getChild("right_arm");
      this.leftArm = root.getChild("left_arm");
      this.rightLeg = root.getChild("right_leg");
      this.leftLeg = root.getChild("left_leg");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -12.0F, -5.5F, 8.0F, 10.0F, 8.0F).texOffs(24, 0).addBox(-1.0F, -5.0F, -7.5F, 2.0F, 4.0F, 2.0F),
         PartPose.offset(0.0F, -7.0F, -2.0F)
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 40)
            .addBox(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F)
            .texOffs(0, 70)
            .addBox(-4.5F, 10.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)),
         PartPose.offset(0.0F, -7.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(60, 21).addBox(-13.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F), PartPose.offset(0.0F, -7.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(60, 58).addBox(9.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F), PartPose.offset(0.0F, -7.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F), PartPose.offset(-4.0F, 11.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F), PartPose.offset(5.0F, 11.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 128, 128);
   }

   public void setupAnim(final IronGolemRenderState state) {
      super.setupAnim(state);
      float attackTick = state.attackTicksRemaining;
      float animationSpeed = state.walkAnimationSpeed;
      float animationPos = state.walkAnimationPos;
      if (attackTick > 0.0F) {
         this.rightArm.xRot = -2.0F + 1.5F * Mth.triangleWave(attackTick, 10.0F);
         this.leftArm.xRot = -2.0F + 1.5F * Mth.triangleWave(attackTick, 10.0F);
      } else {
         int offerFlowerTick = state.offerFlowerTick;
         if (offerFlowerTick > 0) {
            this.rightArm.xRot = -0.8F + 0.025F * Mth.triangleWave(offerFlowerTick, 70.0F);
            this.leftArm.xRot = 0.0F;
         } else {
            this.rightArm.xRot = (-0.2F + 1.5F * Mth.triangleWave(animationPos, 13.0F)) * animationSpeed;
            this.leftArm.xRot = (-0.2F - 1.5F * Mth.triangleWave(animationPos, 13.0F)) * animationSpeed;
         }
      }

      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.rightLeg.xRot = -1.5F * Mth.triangleWave(animationPos, 13.0F) * animationSpeed;
      this.leftLeg.xRot = 1.5F * Mth.triangleWave(animationPos, 13.0F) * animationSpeed;
      this.rightLeg.yRot = 0.0F;
      this.leftLeg.yRot = 0.0F;
   }

   public ModelPart getFlowerHoldingArm() {
      return this.rightArm;
   }
}
