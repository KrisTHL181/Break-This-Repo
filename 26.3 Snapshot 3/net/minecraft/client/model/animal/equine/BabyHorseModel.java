package net.minecraft.client.model.animal.equine;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class BabyHorseModel extends HorseModel {
   public BabyHorseModel(final ModelPart root) {
      super(root);
   }

   public static MeshDefinition createBabyMesh(final CubeDeformation g) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition Body = root.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 13).addBox(-4.0F, -3.5F, -7.0F, 8.0F, 7.0F, 14.0F, g), PartPose.offset(0.0F, 12.5F, 0.0F)
      );
      Body.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(24, 34).addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 8.0F, g),
         PartPose.offsetAndRotation(0.0F, -1.0F, 7.0F, -0.7418F, 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(12, 46).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g), PartPose.offset(2.4F, 16.0F, 5.4F)
      );
      root.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(0, 46).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g), PartPose.offset(-2.4F, 16.0F, 5.4F)
      );
      root.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g), PartPose.offset(2.4F, 16.0F, -5.4F)
      );
      root.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g), PartPose.offset(-2.4F, 16.0F, -5.4F)
      );
      PartDefinition neck = root.addOrReplaceChild(
         "head_parts",
         CubeListBuilder.create().texOffs(30, 0).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 8.0F, 4.0F, g),
         PartPose.offsetAndRotation(0.0F, 10.0F, -6.0F, 0.6109F, 0.0F, 0.0F)
      );
      PartDefinition head = neck.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.9484F, -6.705F, 6.0F, 4.0F, 9.0F, g), PartPose.offset(0.0F, -6.0516F, -0.2951F)
      );
      head.addOrReplaceChild(
         "left_ear",
         CubeListBuilder.create().texOffs(0, 4).addBox(-1.0F, -2.5F, -0.8F, 2.0F, 3.0F, 1.0F, g),
         PartPose.offsetAndRotation(2.0F, -4.2484F, 1.9451F, 0.0F, 0.0F, 0.2618F)
      );
      head.addOrReplaceChild(
         "right_ear",
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 3.0F, 1.0F, g),
         PartPose.offsetAndRotation(-2.0F, -4.2484F, 1.645F, 0.0F, 0.0F, -0.2618F)
      );
      return mesh;
   }

   @Override
   protected float getLegStandingYOffset() {
      return 4.0F;
   }

   @Override
   protected float getLegStandingZOffset() {
      return 0.0F;
   }

   @Override
   protected float getTailXRotOffset() {
      return (float) (-Math.PI / 2);
   }

   @Override
   protected void animateHeadPartsPlacement(final float eating, final float standing) {
      this.headParts.y = this.headParts.y + Mth.lerp(eating, Mth.lerp(standing, 0.0F, -2.0F), 2.0F);
      this.headParts.z = Mth.lerp(standing, this.headParts.z, -4.0F);
   }
}
