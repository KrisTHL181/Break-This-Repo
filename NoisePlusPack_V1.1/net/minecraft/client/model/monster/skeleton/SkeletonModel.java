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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkeletonModel<S extends SkeletonRenderState> extends HumanoidModel<S> {
   public SkeletonModel(ModelPart p_452957_) {
      super(p_452957_);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
      PartDefinition partdefinition = meshdefinition.getRoot();
      createDefaultSkeletonMesh(partdefinition);
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   protected static void createDefaultSkeletonMesh(PartDefinition p_459656_) {
      p_459656_.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-5.0F, 2.0F, 0.0F)
      );
      p_459656_.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(5.0F, 2.0F, 0.0F)
      );
      p_459656_.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      p_459656_.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(2.0F, 12.0F, 0.0F)
      );
   }

   public static LayerDefinition createSingleModelDualBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
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
      partdefinition.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
               .texOffs(0, 0)
               .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
               .texOffs(0, 32)
               .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.2F)),
            PartPose.offset(0.0F, 0.0F, 0.0F)
         )
         .addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create()
            .texOffs(40, 16)
            .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
            .texOffs(42, 33)
            .addBox(-1.55F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F),
         PartPose.offset(-5.5F, 2.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create()
            .texOffs(56, 16)
            .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
            .texOffs(40, 48)
            .addBox(-1.45F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F),
         PartPose.offset(5.5F, 2.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F).texOffs(0, 49).addBox(-1.5F, -0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
         PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F).texOffs(4, 49).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
         PartPose.offset(2.0F, 12.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(S p_454348_) {
      super.setupAnim(p_454348_);
      if (p_454348_.isAggressive && !p_454348_.isHoldingBow) {
         float f = p_454348_.attackTime;
         float f1 = Mth.sin(f * (float) Math.PI);
         float f2 = Mth.sin((1.0F - (1.0F - f) * (1.0F - f)) * (float) Math.PI);
         this.rightArm.zRot = 0.0F;
         this.leftArm.zRot = 0.0F;
         this.rightArm.yRot = -(0.1F - f1 * 0.6F);
         this.leftArm.yRot = 0.1F - f1 * 0.6F;
         this.rightArm.xRot = (float) (-Math.PI / 2);
         this.leftArm.xRot = (float) (-Math.PI / 2);
         this.rightArm.xRot -= f1 * 1.2F - f2 * 0.4F;
         this.leftArm.xRot -= f1 * 1.2F - f2 * 0.4F;
         AnimationUtils.bobArms(this.rightArm, this.leftArm, p_454348_.ageInTicks);
      }
   }

   public void translateToHand(SkeletonRenderState p_452101_, HumanoidArm p_456516_, PoseStack p_460084_) {
      this.root().translateAndRotate(p_460084_);
      float f = p_456516_ == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      ModelPart modelpart = this.getArm(p_456516_);
      modelpart.x += f;
      modelpart.translateAndRotate(p_460084_);
      modelpart.x -= f;
   }
}
