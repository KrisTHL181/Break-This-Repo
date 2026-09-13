package net.minecraft.client.model.animal.sheep;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SheepRenderState;

public class SheepFurModel extends QuadrupedModel<SheepRenderState> {
   public SheepFurModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createFurLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -4.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.6F)),
         PartPose.offset(0.0F, 6.0F, -8.0F)
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(28, 8).addBox(-4.0F, -10.0F, -7.0F, 8.0F, 16.0F, 6.0F, new CubeDeformation(1.75F)),
         PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.5F));
      root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.0F, 12.0F, 7.0F));
      root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(3.0F, 12.0F, 7.0F));
      root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.0F, 12.0F, -5.0F));
      root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(3.0F, 12.0F, -5.0F));
      return LayerDefinition.create(mesh, 64, 32);
   }

   public void setupAnim(final SheepRenderState state) {
      super.setupAnim(state);
      this.head.y = this.head.y + state.headEatPositionScale * 9.0F * state.ageScale;
      this.head.xRot = state.headEatAngleScale;
   }
}
