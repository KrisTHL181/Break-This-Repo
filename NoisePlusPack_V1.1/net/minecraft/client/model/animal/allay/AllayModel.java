package net.minecraft.client.model.animal.allay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AllayModel extends EntityModel<AllayRenderState> implements ArmedModel<AllayRenderState> {
   private final ModelPart head = this.root.getChild("head");
   private final ModelPart body = this.root.getChild("body");
   private final ModelPart right_arm = this.body.getChild("right_arm");
   private final ModelPart left_arm = this.body.getChild("left_arm");
   private final ModelPart right_wing = this.body.getChild("right_wing");
   private final ModelPart left_wing = this.body.getChild("left_wing");
   private static final float FLYING_ANIMATION_X_ROT = (float) (Math.PI / 4);
   private static final float MAX_HAND_HOLDING_ITEM_X_ROT_RAD = -1.134464F;
   private static final float MIN_HAND_HOLDING_ITEM_X_ROT_RAD = (float) (-Math.PI / 3);

   public AllayModel(ModelPart p_454245_) {
      super(p_454245_.getChild("root"), RenderTypes::entityTranslucent);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 23.5F, 0.0F));
      partdefinition1.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -3.99F, 0.0F)
      );
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 10)
            .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 16)
            .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(-0.2F)),
         PartPose.offset(0.0F, -4.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(23, 0).addBox(-0.75F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(-0.01F)),
         PartPose.offset(-1.75F, 0.5F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(23, 6).addBox(-0.25F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(-0.01F)),
         PartPose.offset(1.75F, 0.5F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().texOffs(16, 14).addBox(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-0.5F, 0.0F, 0.6F)
      );
      partdefinition2.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().texOffs(16, 14).addBox(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.5F, 0.0F, 0.6F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(AllayRenderState p_451451_) {
      super.setupAnim(p_451451_);
      float f = p_451451_.walkAnimationSpeed;
      float f1 = p_451451_.walkAnimationPos;
      float f2 = p_451451_.ageInTicks * 20.0F * (float) (Math.PI / 180.0) + f1;
      float f3 = Mth.cos(f2) * (float) Math.PI * 0.15F + f;
      float f4 = p_451451_.ageInTicks * 9.0F * (float) (Math.PI / 180.0);
      float f5 = Math.min(f / 0.3F, 1.0F);
      float f6 = 1.0F - f5;
      float f7 = p_451451_.holdingAnimationProgress;
      if (p_451451_.isDancing) {
         float f8 = p_451451_.ageInTicks * 8.0F * (float) (Math.PI / 180.0) + f;
         float f9 = Mth.cos(f8) * 16.0F * (float) (Math.PI / 180.0);
         float f10 = p_451451_.spinningProgress;
         float f11 = Mth.cos(f8) * 14.0F * (float) (Math.PI / 180.0);
         float f12 = Mth.cos(f8) * 30.0F * (float) (Math.PI / 180.0);
         this.root.yRot = p_451451_.isSpinning ? (float) (Math.PI * 4) * f10 : this.root.yRot;
         this.root.zRot = f9 * (1.0F - f10);
         this.head.yRot = f12 * (1.0F - f10);
         this.head.zRot = f11 * (1.0F - f10);
      } else {
         this.head.xRot = p_451451_.xRot * (float) (Math.PI / 180.0);
         this.head.yRot = p_451451_.yRot * (float) (Math.PI / 180.0);
      }

      this.right_wing.xRot = 0.43633232F * (1.0F - f5);
      this.right_wing.yRot = (float) (-Math.PI / 4) + f3;
      this.left_wing.xRot = 0.43633232F * (1.0F - f5);
      this.left_wing.yRot = (float) (Math.PI / 4) - f3;
      this.body.xRot = f5 * (float) (Math.PI / 4);
      float f13 = f7 * Mth.lerp(f5, (float) (-Math.PI / 3), -1.134464F);
      this.root.y = this.root.y + (float)Math.cos(f4) * 0.25F * f6;
      this.right_arm.xRot = f13;
      this.left_arm.xRot = f13;
      float f14 = f6 * (1.0F - f7);
      float f15 = 0.43633232F - Mth.cos(f4 + (float) (Math.PI * 3.0 / 2.0)) * (float) Math.PI * 0.075F * f14;
      this.left_arm.zRot = -f15;
      this.right_arm.zRot = f15;
      this.right_arm.yRot = 0.27925268F * f7;
      this.left_arm.yRot = -0.27925268F * f7;
   }

   public void translateToHand(AllayRenderState p_459683_, HumanoidArm p_450602_, PoseStack p_453587_) {
      float f = 1.0F;
      float f1 = 3.0F;
      this.root.translateAndRotate(p_453587_);
      this.body.translateAndRotate(p_453587_);
      p_453587_.translate(0.0F, 0.0625F, 0.1875F);
      p_453587_.mulPose(Axis.XP.rotation(this.right_arm.xRot));
      p_453587_.scale(0.7F, 0.7F, 0.7F);
      p_453587_.translate(0.0625F, 0.0F, 0.0F);
   }
}
