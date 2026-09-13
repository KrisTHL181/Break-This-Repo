package net.minecraft.client.model.animal.ghast;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.ghast.GhastModel;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;

public class HappyGhastModel extends EntityModel<HappyGhastRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.2375F);
   private static final float BODY_SQUEEZE = 0.9375F;
   private final ModelPart[] tentacles = new ModelPart[9];
   private final ModelPart body;

   public HappyGhastModel(final ModelPart root) {
      super(root);
      this.body = root.getChild("body");

      for (int i = 0; i < this.tentacles.length; i++) {
         this.tentacles[i] = this.body.getChild(PartNames.tentacle(i));
      }
   }

   public static LayerDefinition createBodyLayer(final boolean isBaby, final CubeDeformation deformation) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, deformation), PartPose.offset(0.0F, 16.0F, 0.0F)
      );
      if (isBaby) {
         body.addOrReplaceChild(
            "inner_body",
            CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F, deformation.extend(-0.5F)),
            PartPose.offset(0.0F, 8.0F, 0.0F)
         );
      }

      body.addOrReplaceChild(
         PartNames.tentacle(0),
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, deformation),
         PartPose.offset(-3.75F, 7.0F, -5.0F)
      );
      body.addOrReplaceChild(
         PartNames.tentacle(1),
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, deformation),
         PartPose.offset(1.25F, 7.0F, -5.0F)
      );
      body.addOrReplaceChild(
         PartNames.tentacle(2),
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, deformation),
         PartPose.offset(6.25F, 7.0F, -5.0F)
      );
      body.addOrReplaceChild(
         PartNames.tentacle(3),
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, deformation),
         PartPose.offset(-6.25F, 7.0F, 0.0F)
      );
      body.addOrReplaceChild(
         PartNames.tentacle(4),
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, deformation),
         PartPose.offset(-1.25F, 7.0F, 0.0F)
      );
      body.addOrReplaceChild(
         PartNames.tentacle(5),
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, deformation),
         PartPose.offset(3.75F, 7.0F, 0.0F)
      );
      body.addOrReplaceChild(
         PartNames.tentacle(6),
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, deformation),
         PartPose.offset(-3.75F, 7.0F, 5.0F)
      );
      body.addOrReplaceChild(
         PartNames.tentacle(7),
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, deformation),
         PartPose.offset(1.25F, 7.0F, 5.0F)
      );
      body.addOrReplaceChild(
         PartNames.tentacle(8),
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, deformation),
         PartPose.offset(6.25F, 7.0F, 5.0F)
      );
      return LayerDefinition.create(mesh, 64, 64).apply(MeshTransformer.scaling(4.0F));
   }

   public void setupAnim(final HappyGhastRenderState state) {
      super.setupAnim(state);
      if (!state.bodyItem.isEmpty()) {
         this.body.xScale = 0.9375F;
         this.body.yScale = 0.9375F;
         this.body.zScale = 0.9375F;
      }

      GhastModel.animateTentacles(state, this.tentacles);
   }
}
