package net.minecraft.client.model.animal.sniffer;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.SnifferAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;

public class SniffletModel extends SnifferModel {
   private static final String LOWER_BEAK = "lower_beak";
   private final KeyframeAnimation babyTransform;

   public SniffletModel(final ModelPart root) {
      super(root);
      this.babyTransform = SnifferAnimation.BABY_TRANSFORM.bake(root);
   }

   @Override
   public void setupAnim(final SnifferRenderState state) {
      super.setupAnim(state);
      this.babyTransform.applyStatic();
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition body = bone.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 35)
            .addBox(-13.0F, -14.0F, -0.5F, 14.0F, 14.0F, 20.0F, new CubeDeformation(0.25F))
            .texOffs(0, 0)
            .addBox(-13.0F, -14.0F, -0.5F, 14.0F, 15.0F, 20.0F)
            .texOffs(68, 0)
            .addBox(-13.0F, 0.0F, -0.5F, 14.0F, 0.0F, 20.0F),
         PartPose.offset(6.0F, -3.0F, -9.5F)
      );
      PartDefinition head = body.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(68, 20)
            .addBox(-5.0F, -4.25F, -7.5F, 10.0F, 9.0F, 9.0F)
            .texOffs(88, 20)
            .addBox(-5.0F, 3.75F, -7.5F, 10.0F, 0.0F, 9.0F),
         PartPose.offset(-6.0F, -4.75F, 0.0F)
      );
      head.addOrReplaceChild(
         "left_ear", CubeListBuilder.create().texOffs(104, 38).addBox(0.0F, 0.0F, -2.0F, 1.0F, 11.0F, 3.0F), PartPose.offset(5.0F, -4.25F, -1.5F)
      );
      head.addOrReplaceChild(
         "right_ear", CubeListBuilder.create().texOffs(96, 38).addBox(-1.0F, 0.0F, -2.0F, 1.0F, 11.0F, 3.0F), PartPose.offset(-5.0F, -4.25F, -1.5F)
      );
      head.addOrReplaceChild(
         "nose", CubeListBuilder.create().texOffs(68, 47).addBox(-5.0F, -3.0F, -2.0F, 10.0F, 3.0F, 4.0F), PartPose.offset(0.0F, -1.25F, -9.5F)
      );
      head.addOrReplaceChild(
         "lower_beak", CubeListBuilder.create().texOffs(68, 38).addBox(-5.0F, -2.5F, -2.0F, 10.0F, 5.0F, 4.0F), PartPose.offset(0.0F, 1.25F, -9.5F)
      );
      bone.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(0, 69).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F), PartPose.offset(-4.0F, -4.0F, -7.0F)
      );
      bone.addOrReplaceChild(
         "right_mid_leg", CubeListBuilder.create().texOffs(0, 78).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F), PartPose.offset(-4.0F, -4.0F, 0.0F)
      );
      bone.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(0, 87).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F), PartPose.offset(-4.0F, -4.0F, 7.0F)
      );
      bone.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(16, 69).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F), PartPose.offset(4.0F, -4.0F, -7.0F)
      );
      bone.addOrReplaceChild(
         "left_mid_leg", CubeListBuilder.create().texOffs(16, 78).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F), PartPose.offset(4.0F, -4.0F, 0.0F)
      );
      bone.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(16, 87).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F), PartPose.offset(4.0F, -4.0F, 7.0F)
      );
      return LayerDefinition.create(mesh, 128, 128);
   }
}
