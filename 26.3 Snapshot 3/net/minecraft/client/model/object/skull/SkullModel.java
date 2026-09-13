package net.minecraft.client.model.object.skull;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class SkullModel extends SkullModelBase {
   protected final ModelPart head;

   public SkullModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");
   }

   public static MeshDefinition createHeadModel() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      return mesh;
   }

   public static LayerDefinition createHumanoidHeadLayer() {
      MeshDefinition mesh = createHeadModel();
      PartDefinition root = mesh.getRoot();
      root.getChild("head")
         .addOrReplaceChild(
            "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.25F)), PartPose.ZERO
         );
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition createMobHeadLayer() {
      MeshDefinition mesh = createHeadModel();
      return LayerDefinition.create(mesh, 64, 32);
   }

   public void setupAnim(final SkullModelBase.State state) {
      super.setupAnim(state);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
   }
}
