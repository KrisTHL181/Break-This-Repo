package net.minecraft.client.model.animal.camel;

import net.minecraft.client.animation.definitions.CamelBabyAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BabyCamelModel extends CamelModel {
   public BabyCamelModel(final ModelPart root) {
      super(
         root,
         CamelBabyAnimation.CAMEL_BABY_WALK,
         CamelBabyAnimation.CAMEL_BABY_SIT,
         CamelBabyAnimation.CAMEL_BABY_SIT_POSE,
         CamelBabyAnimation.CAMEL_BABY_STANDUP,
         CamelBabyAnimation.CAMEL_BABY_IDLE,
         CamelBabyAnimation.CAMEL_BABY_DASH
      );
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 14).addBox(-4.5F, -4.0F, -8.0F, 9.0F, 8.0F, 16.0F), PartPose.offset(0.0F, 7.0F, 0.0F)
      );
      body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(50, 38).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 9.0F, 0.0F), PartPose.offset(0.0F, -1.5F, 8.05F));
      PartDefinition head = body.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(20, 0)
            .addBox(-2.5F, -3.0F, -7.5F, 5.0F, 5.0F, 7.0F)
            .texOffs(0, 0)
            .addBox(-2.5F, -12.0F, -7.5F, 5.0F, 9.0F, 5.0F)
            .texOffs(0, 14)
            .addBox(-2.5F, -12.0F, -10.5F, 5.0F, 4.0F, 3.0F),
         PartPose.offset(0.0F, 1.0F, -7.5F)
      );
      head.addOrReplaceChild(
         "right_ear", CubeListBuilder.create().texOffs(37, 0).addBox(-3.0F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(-2.5F, -11.0F, -4.0F)
      );
      head.addOrReplaceChild(
         "left_ear", CubeListBuilder.create().texOffs(47, 0).addBox(0.0F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(2.5F, -11.0F, -4.0F)
      );
      root.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(36, 14).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 13.0F, 3.0F), PartPose.offset(-3.0F, 11.5F, -5.5F)
      );
      root.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(48, 14).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 13.0F, 3.0F), PartPose.offset(3.0F, 11.5F, -5.5F)
      );
      root.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(12, 38).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 13.0F, 3.0F), PartPose.offset(3.0F, 11.5F, 5.5F)
      );
      root.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(0, 38).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 13.0F, 3.0F), PartPose.offset(-3.0F, 11.5F, 5.5F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }
}
