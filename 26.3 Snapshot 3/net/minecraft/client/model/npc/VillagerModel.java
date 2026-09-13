package net.minecraft.client.model.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.util.Mth;

public class VillagerModel extends EntityModel<VillagerRenderState> implements HeadedModel, VillagerLikeModel<VillagerRenderState> {
   private final ModelPart head;
   private final ModelPart rightLeg;
   private final ModelPart leftLeg;
   private final ModelPart arms;

   public VillagerModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");
      this.rightLeg = root.getChild("right_leg");
      this.leftLeg = root.getChild("left_leg");
      this.arms = root.getChild("arms");
   }

   public static MeshDefinition createBodyModel() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      float offset = 0.5F;
      PartDefinition head = root.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO
      );
      PartDefinition hat = head.addOrReplaceChild(
         "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)), PartPose.ZERO
      );
      hat.addOrReplaceChild(
         "hat_rim",
         CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F),
         PartPose.rotation((float) (-Math.PI / 2), 0.0F, 0.0F)
      );
      head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
      PartDefinition body = root.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F), PartPose.ZERO
      );
      body.addOrReplaceChild(
         "jacket", CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO
      );
      root.addOrReplaceChild(
         "arms",
         CubeListBuilder.create()
            .texOffs(44, 22)
            .addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
            .texOffs(44, 22)
            .addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, true)
            .texOffs(40, 38)
            .addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
         PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F)
      );
      return mesh;
   }

   public static MeshDefinition createNoHatModel() {
      MeshDefinition mesh = createBodyModel();
      mesh.getRoot().clearChild("head").clearRecursively();
      return mesh;
   }

   public void setupAnim(final VillagerRenderState state) {
      super.setupAnim(state);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      if (state.isUnhappy) {
         this.head.zRot = 0.3F * Mth.sin(0.45F * state.ageInTicks);
         this.head.xRot = 0.4F;
      } else {
         this.head.zRot = 0.0F;
      }

      this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed * 0.5F;
      this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F * state.walkAnimationSpeed * 0.5F;
      this.rightLeg.yRot = 0.0F;
      this.leftLeg.yRot = 0.0F;
   }

   @Override
   public ModelPart getHead() {
      return this.head;
   }

   public void translateToArms(final VillagerRenderState state, final PoseStack outputPoseStack) {
      this.root.translateAndRotate(outputPoseStack);
      this.arms.translateAndRotate(outputPoseStack);
   }
}
