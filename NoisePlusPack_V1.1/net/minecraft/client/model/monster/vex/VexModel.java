package net.minecraft.client.model.monster.vex;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VexModel extends EntityModel<VexRenderState> implements ArmedModel<VexRenderState> {
   private final ModelPart body = this.root.getChild("body");
   private final ModelPart rightArm = this.body.getChild("right_arm");
   private final ModelPart leftArm = this.body.getChild("left_arm");
   private final ModelPart rightWing = this.body.getChild("right_wing");
   private final ModelPart leftWing = this.body.getChild("left_wing");
   private final ModelPart head = this.root.getChild("head");

   public VexModel(ModelPart p_452203_) {
      super(p_452203_.getChild("root"), RenderTypes::entityTranslucent);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, -2.5F, 0.0F));
      partdefinition1.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 10)
            .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 16)
            .addBox(-1.5F, 1.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(-0.2F)),
         PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(23, 0).addBox(-1.25F, -0.5F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(-1.75F, 0.25F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(23, 6).addBox(-0.75F, -0.5F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(1.75F, 0.25F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().texOffs(16, 14).mirror().addBox(0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.5F, 1.0F, 1.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().texOffs(16, 14).addBox(0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-0.5F, 1.0F, 1.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(VexRenderState p_456194_) {
      super.setupAnim(p_456194_);
      this.head.yRot = p_456194_.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = p_456194_.xRot * (float) (Math.PI / 180.0);
      float f = Mth.cos(p_456194_.ageInTicks * 5.5F * (float) (Math.PI / 180.0)) * 0.1F;
      this.rightArm.zRot = (float) (Math.PI / 5) + f;
      this.leftArm.zRot = -((float) (Math.PI / 5) + f);
      if (p_456194_.isCharging) {
         this.body.xRot = 0.0F;
         this.setArmsCharging(!p_456194_.rightHandItemState.isEmpty(), !p_456194_.leftHandItemState.isEmpty(), f);
      } else {
         this.body.xRot = (float) (Math.PI / 20);
      }

      this.leftWing.yRot = 1.0995574F + Mth.cos(p_456194_.ageInTicks * 45.836624F * (float) (Math.PI / 180.0)) * (float) (Math.PI / 180.0) * 16.2F;
      this.rightWing.yRot = -this.leftWing.yRot;
      this.leftWing.xRot = 0.47123888F;
      this.leftWing.zRot = -0.47123888F;
      this.rightWing.xRot = 0.47123888F;
      this.rightWing.zRot = 0.47123888F;
   }

   private void setArmsCharging(boolean p_456462_, boolean p_454818_, float p_458825_) {
      if (!p_456462_ && !p_454818_) {
         this.rightArm.xRot = -1.2217305F;
         this.rightArm.yRot = (float) (Math.PI / 12);
         this.rightArm.zRot = -0.47123888F - p_458825_;
         this.leftArm.xRot = -1.2217305F;
         this.leftArm.yRot = (float) (-Math.PI / 12);
         this.leftArm.zRot = 0.47123888F + p_458825_;
      } else {
         if (p_456462_) {
            this.rightArm.xRot = (float) (Math.PI * 7.0 / 6.0);
            this.rightArm.yRot = (float) (Math.PI / 12);
            this.rightArm.zRot = -0.47123888F - p_458825_;
         }

         if (p_454818_) {
            this.leftArm.xRot = (float) (Math.PI * 7.0 / 6.0);
            this.leftArm.yRot = (float) (-Math.PI / 12);
            this.leftArm.zRot = 0.47123888F + p_458825_;
         }
      }
   }

   public void translateToHand(VexRenderState p_450432_, HumanoidArm p_452397_, PoseStack p_460231_) {
      boolean flag = p_452397_ == HumanoidArm.RIGHT;
      ModelPart modelpart = flag ? this.rightArm : this.leftArm;
      this.root.translateAndRotate(p_460231_);
      this.body.translateAndRotate(p_460231_);
      modelpart.translateAndRotate(p_460231_);
      p_460231_.scale(0.55F, 0.55F, 0.55F);
      this.offsetStackPosition(p_460231_, flag);
   }

   private void offsetStackPosition(PoseStack p_454555_, boolean p_458216_) {
      if (p_458216_) {
         p_454555_.translate(0.046875, -0.15625, 0.078125);
      } else {
         p_454555_.translate(-0.046875, -0.15625, 0.078125);
      }
   }
}
