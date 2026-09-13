package net.minecraft.client.model.animal.fish;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public class CodModel extends EntityModel<LivingEntityRenderState> {
   private final ModelPart tailFin;

   public CodModel(final ModelPart root) {
      super(root);
      this.tailFin = root.getChild("tail_fin");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      int yo = 22;
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 4.0F, 7.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
      root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(11, 0).addBox(-1.0F, -2.0F, -3.0F, 2.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
      root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 22.0F, -3.0F));
      root.addOrReplaceChild(
         "right_fin",
         CubeListBuilder.create().texOffs(22, 1).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F),
         PartPose.offsetAndRotation(-1.0F, 23.0F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 4))
      );
      root.addOrReplaceChild(
         "left_fin",
         CubeListBuilder.create().texOffs(22, 4).addBox(0.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F),
         PartPose.offsetAndRotation(1.0F, 23.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 4))
      );
      root.addOrReplaceChild(
         "tail_fin", CubeListBuilder.create().texOffs(22, 3).addBox(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 4.0F), PartPose.offset(0.0F, 22.0F, 7.0F)
      );
      root.addOrReplaceChild(
         "top_fin", CubeListBuilder.create().texOffs(20, -6).addBox(0.0F, -1.0F, -1.0F, 0.0F, 1.0F, 6.0F), PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 32, 32);
   }

   public void setupAnim(final LivingEntityRenderState state) {
      super.setupAnim(state);
      float amplitudeMultiplier = state.isInWater ? 1.0F : 1.5F;
      this.tailFin.yRot = -amplitudeMultiplier * 0.45F * Mth.sin(0.6F * state.ageInTicks);
   }
}
