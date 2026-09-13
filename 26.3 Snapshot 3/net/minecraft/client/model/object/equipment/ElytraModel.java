package net.minecraft.client.model.object.equipment;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class ElytraModel extends EntityModel<HumanoidRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5F);
   private final ModelPart rightWing;
   private final ModelPart leftWing;

   public ElytraModel(final ModelPart root) {
      super(root);
      this.leftWing = root.getChild("left_wing");
      this.rightWing = root.getChild("right_wing");
   }

   public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      CubeDeformation windDeformation = new CubeDeformation(1.0F);
      root.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().texOffs(22, 0).addBox(-10.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, windDeformation),
         PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, (float) (Math.PI / 12), 0.0F, (float) (-Math.PI / 12))
      );
      root.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, windDeformation),
         PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, (float) (Math.PI / 12), 0.0F, (float) (Math.PI / 12))
      );
      return LayerDefinition.create(mesh, 64, 32);
   }

   public void setupAnim(final HumanoidRenderState state) {
      super.setupAnim(state);
      this.leftWing.y = state.isCrouching ? 3.0F : 0.0F;
      this.leftWing.xRot = state.elytraRotX;
      this.leftWing.zRot = state.elytraRotZ;
      this.leftWing.yRot = state.elytraRotY;
      this.rightWing.yRot = -this.leftWing.yRot;
      this.rightWing.y = this.leftWing.y;
      this.rightWing.xRot = this.leftWing.xRot;
      this.rightWing.zRot = -this.leftWing.zRot;
   }
}
