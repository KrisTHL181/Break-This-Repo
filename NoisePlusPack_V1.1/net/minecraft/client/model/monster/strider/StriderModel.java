package net.minecraft.client.model.monster.strider;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.StriderRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StriderModel extends EntityModel<StriderRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5F);
   private static final String RIGHT_BOTTOM_BRISTLE = "right_bottom_bristle";
   private static final String RIGHT_MIDDLE_BRISTLE = "right_middle_bristle";
   private static final String RIGHT_TOP_BRISTLE = "right_top_bristle";
   private static final String LEFT_TOP_BRISTLE = "left_top_bristle";
   private static final String LEFT_MIDDLE_BRISTLE = "left_middle_bristle";
   private static final String LEFT_BOTTOM_BRISTLE = "left_bottom_bristle";
   private final ModelPart rightLeg;
   private final ModelPart leftLeg;
   private final ModelPart body;
   private final ModelPart rightBottomBristle;
   private final ModelPart rightMiddleBristle;
   private final ModelPart rightTopBristle;
   private final ModelPart leftTopBristle;
   private final ModelPart leftMiddleBristle;
   private final ModelPart leftBottomBristle;

   public StriderModel(ModelPart p_453532_) {
      super(p_453532_);
      this.rightLeg = p_453532_.getChild("right_leg");
      this.leftLeg = p_453532_.getChild("left_leg");
      this.body = p_453532_.getChild("body");
      this.rightBottomBristle = this.body.getChild("right_bottom_bristle");
      this.rightMiddleBristle = this.body.getChild("right_middle_bristle");
      this.rightTopBristle = this.body.getChild("right_top_bristle");
      this.leftTopBristle = this.body.getChild("left_top_bristle");
      this.leftMiddleBristle = this.body.getChild("left_middle_bristle");
      this.leftBottomBristle = this.body.getChild("left_bottom_bristle");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 16.0F, 4.0F), PartPose.offset(-4.0F, 8.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 55).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 16.0F, 4.0F), PartPose.offset(4.0F, 8.0F, 0.0F)
      );
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -6.0F, -8.0F, 16.0F, 14.0F, 16.0F), PartPose.offset(0.0F, 1.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
         "right_bottom_bristle",
         CubeListBuilder.create().texOffs(16, 65).addBox(-12.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F, true),
         PartPose.offsetAndRotation(-8.0F, 4.0F, -8.0F, 0.0F, 0.0F, -1.2217305F)
      );
      partdefinition1.addOrReplaceChild(
         "right_middle_bristle",
         CubeListBuilder.create().texOffs(16, 49).addBox(-12.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F, true),
         PartPose.offsetAndRotation(-8.0F, -1.0F, -8.0F, 0.0F, 0.0F, -1.134464F)
      );
      partdefinition1.addOrReplaceChild(
         "right_top_bristle",
         CubeListBuilder.create().texOffs(16, 33).addBox(-12.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F, true),
         PartPose.offsetAndRotation(-8.0F, -5.0F, -8.0F, 0.0F, 0.0F, -0.87266463F)
      );
      partdefinition1.addOrReplaceChild(
         "left_top_bristle",
         CubeListBuilder.create().texOffs(16, 33).addBox(0.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F),
         PartPose.offsetAndRotation(8.0F, -6.0F, -8.0F, 0.0F, 0.0F, 0.87266463F)
      );
      partdefinition1.addOrReplaceChild(
         "left_middle_bristle",
         CubeListBuilder.create().texOffs(16, 49).addBox(0.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F),
         PartPose.offsetAndRotation(8.0F, -2.0F, -8.0F, 0.0F, 0.0F, 1.134464F)
      );
      partdefinition1.addOrReplaceChild(
         "left_bottom_bristle",
         CubeListBuilder.create().texOffs(16, 65).addBox(0.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F),
         PartPose.offsetAndRotation(8.0F, 3.0F, -8.0F, 0.0F, 0.0F, 1.2217305F)
      );
      return LayerDefinition.create(meshdefinition, 64, 128);
   }

   public void setupAnim(StriderRenderState p_456619_) {
      super.setupAnim(p_456619_);
      float f = p_456619_.walkAnimationPos;
      float f1 = Math.min(p_456619_.walkAnimationSpeed, 0.25F);
      if (!p_456619_.isRidden) {
         this.body.xRot = p_456619_.xRot * (float) (Math.PI / 180.0);
         this.body.yRot = p_456619_.yRot * (float) (Math.PI / 180.0);
      } else {
         this.body.xRot = 0.0F;
         this.body.yRot = 0.0F;
      }

      float f2 = 1.5F;
      this.body.zRot = 0.1F * Mth.sin(f * 1.5F) * 4.0F * f1;
      this.body.y = 2.0F;
      this.body.y = this.body.y - 2.0F * Mth.cos(f * 1.5F) * 2.0F * f1;
      this.leftLeg.xRot = Mth.sin(f * 1.5F * 0.5F) * 2.0F * f1;
      this.rightLeg.xRot = Mth.sin(f * 1.5F * 0.5F + (float) Math.PI) * 2.0F * f1;
      this.leftLeg.zRot = (float) (Math.PI / 18) * Mth.cos(f * 1.5F * 0.5F) * f1;
      this.rightLeg.zRot = (float) (Math.PI / 18) * Mth.cos(f * 1.5F * 0.5F + (float) Math.PI) * f1;
      this.leftLeg.y = 8.0F + 2.0F * Mth.sin(f * 1.5F * 0.5F + (float) Math.PI) * 2.0F * f1;
      this.rightLeg.y = 8.0F + 2.0F * Mth.sin(f * 1.5F * 0.5F) * 2.0F * f1;
      this.rightBottomBristle.zRot = -1.2217305F;
      this.rightMiddleBristle.zRot = -1.134464F;
      this.rightTopBristle.zRot = -0.87266463F;
      this.leftTopBristle.zRot = 0.87266463F;
      this.leftMiddleBristle.zRot = 1.134464F;
      this.leftBottomBristle.zRot = 1.2217305F;
      float f3 = Mth.cos(f * 1.5F + (float) Math.PI) * f1;
      this.rightBottomBristle.zRot += f3 * 1.3F;
      this.rightMiddleBristle.zRot += f3 * 1.2F;
      this.rightTopBristle.zRot += f3 * 0.6F;
      this.leftTopBristle.zRot += f3 * 0.6F;
      this.leftMiddleBristle.zRot += f3 * 1.2F;
      this.leftBottomBristle.zRot += f3 * 1.3F;
      float f4 = 1.0F;
      float f5 = 1.0F;
      this.rightBottomBristle.zRot = this.rightBottomBristle.zRot + 0.05F * Mth.sin(p_456619_.ageInTicks * 1.0F * -0.4F);
      this.rightMiddleBristle.zRot = this.rightMiddleBristle.zRot + 0.1F * Mth.sin(p_456619_.ageInTicks * 1.0F * 0.2F);
      this.rightTopBristle.zRot = this.rightTopBristle.zRot + 0.1F * Mth.sin(p_456619_.ageInTicks * 1.0F * 0.4F);
      this.leftTopBristle.zRot = this.leftTopBristle.zRot + 0.1F * Mth.sin(p_456619_.ageInTicks * 1.0F * 0.4F);
      this.leftMiddleBristle.zRot = this.leftMiddleBristle.zRot + 0.1F * Mth.sin(p_456619_.ageInTicks * 1.0F * 0.2F);
      this.leftBottomBristle.zRot = this.leftBottomBristle.zRot + 0.05F * Mth.sin(p_456619_.ageInTicks * 1.0F * -0.4F);
   }
}
