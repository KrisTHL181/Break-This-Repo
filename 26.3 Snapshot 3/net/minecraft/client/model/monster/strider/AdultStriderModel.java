package net.minecraft.client.model.monster.strider;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class AdultStriderModel extends StriderModel {
   private static final String RIGHT_BOTTOM_BRISTLE = "right_bottom_bristle";
   private static final String RIGHT_MIDDLE_BRISTLE = "right_middle_bristle";
   private static final String RIGHT_TOP_BRISTLE = "right_top_bristle";
   private static final String LEFT_TOP_BRISTLE = "left_top_bristle";
   private static final String LEFT_MIDDLE_BRISTLE = "left_middle_bristle";
   private static final String LEFT_BOTTOM_BRISTLE = "left_bottom_bristle";
   private final ModelPart rightBottomBristle = this.body.getChild("right_bottom_bristle");
   private final ModelPart rightMiddleBristle = this.body.getChild("right_middle_bristle");
   private final ModelPart rightTopBristle = this.body.getChild("right_top_bristle");
   private final ModelPart leftTopBristle = this.body.getChild("left_top_bristle");
   private final ModelPart leftMiddleBristle = this.body.getChild("left_middle_bristle");
   private final ModelPart leftBottomBristle = this.body.getChild("left_bottom_bristle");

   public AdultStriderModel(final ModelPart root) {
      super(root);
   }

   @Override
   protected void customAnimations(final float animationPos, final float animationSpeed, final float ageInTicks) {
      this.rightBottomBristle.zRot = -1.2217305F;
      this.rightMiddleBristle.zRot = -1.134464F;
      this.rightTopBristle.zRot = -0.87266463F;
      this.leftTopBristle.zRot = 0.87266463F;
      this.leftMiddleBristle.zRot = 1.134464F;
      this.leftBottomBristle.zRot = 1.2217305F;
      float bristleFlow = Mth.cos(animationPos * 1.5F + (float) Math.PI) * animationSpeed;
      this.animateBristle(
         ageInTicks,
         bristleFlow,
         this.rightTopBristle,
         this.rightMiddleBristle,
         this.rightBottomBristle,
         (modelPart, rotation) -> modelPart.zRot = modelPart.zRot + rotation
      );
      this.animateBristle(
         ageInTicks,
         bristleFlow,
         this.leftTopBristle,
         this.leftMiddleBristle,
         this.leftBottomBristle,
         (modelPart, rotation) -> modelPart.zRot = modelPart.zRot + rotation
      );
      this.body.y = 2.0F;
      this.body.y = this.body.y - 2.0F * Mth.cos(animationPos * 1.5F) * 2.0F * animationSpeed;
      this.leftLeg.y = 8.0F + 2.0F * Mth.sin(animationPos * 1.5F * 0.5F + (float) Math.PI) * 2.0F * animationSpeed;
      this.rightLeg.y = 8.0F + 2.0F * Mth.sin(animationPos * 1.5F * 0.5F) * 2.0F * animationSpeed;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 16.0F, 4.0F), PartPose.offset(-4.0F, 8.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 55).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 16.0F, 4.0F), PartPose.offset(4.0F, 8.0F, 0.0F)
      );
      PartDefinition body = root.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -6.0F, -8.0F, 16.0F, 14.0F, 16.0F), PartPose.offset(0.0F, 1.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "right_bottom_bristle",
         CubeListBuilder.create().texOffs(16, 65).addBox(-12.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F, true),
         PartPose.offsetAndRotation(-8.0F, 4.0F, -8.0F, 0.0F, 0.0F, -1.2217305F)
      );
      body.addOrReplaceChild(
         "right_middle_bristle",
         CubeListBuilder.create().texOffs(16, 49).addBox(-12.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F, true),
         PartPose.offsetAndRotation(-8.0F, -1.0F, -8.0F, 0.0F, 0.0F, -1.134464F)
      );
      body.addOrReplaceChild(
         "right_top_bristle",
         CubeListBuilder.create().texOffs(16, 33).addBox(-12.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F, true),
         PartPose.offsetAndRotation(-8.0F, -5.0F, -8.0F, 0.0F, 0.0F, -0.87266463F)
      );
      body.addOrReplaceChild(
         "left_top_bristle",
         CubeListBuilder.create().texOffs(16, 33).addBox(0.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F),
         PartPose.offsetAndRotation(8.0F, -6.0F, -8.0F, 0.0F, 0.0F, 0.87266463F)
      );
      body.addOrReplaceChild(
         "left_middle_bristle",
         CubeListBuilder.create().texOffs(16, 49).addBox(0.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F),
         PartPose.offsetAndRotation(8.0F, -2.0F, -8.0F, 0.0F, 0.0F, 1.134464F)
      );
      body.addOrReplaceChild(
         "left_bottom_bristle",
         CubeListBuilder.create().texOffs(16, 65).addBox(0.0F, 0.0F, 0.0F, 12.0F, 0.0F, 16.0F),
         PartPose.offsetAndRotation(8.0F, 3.0F, -8.0F, 0.0F, 0.0F, 1.2217305F)
      );
      return LayerDefinition.create(mesh, 64, 128);
   }
}
