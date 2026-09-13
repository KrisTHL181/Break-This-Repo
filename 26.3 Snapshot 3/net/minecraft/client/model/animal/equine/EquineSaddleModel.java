package net.minecraft.client.model.animal.equine;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;

public class EquineSaddleModel extends AbstractEquineModel<EquineRenderState> {
   private static final String SADDLE = "saddle";
   private static final String LEFT_SADDLE_MOUTH = "left_saddle_mouth";
   private static final String LEFT_SADDLE_LINE = "left_saddle_line";
   private static final String RIGHT_SADDLE_MOUTH = "right_saddle_mouth";
   private static final String RIGHT_SADDLE_LINE = "right_saddle_line";
   private static final String HEAD_SADDLE = "head_saddle";
   private static final String MOUTH_SADDLE_WRAP = "mouth_saddle_wrap";
   private final ModelPart[] ridingParts;

   public EquineSaddleModel(final ModelPart root) {
      super(root);
      ModelPart leftSaddleLine = this.headParts.getChild("left_saddle_line");
      ModelPart rightSaddleLine = this.headParts.getChild("right_saddle_line");
      this.ridingParts = new ModelPart[]{leftSaddleLine, rightSaddleLine};
   }

   public static LayerDefinition createSaddleLayer() {
      MeshDefinition mesh = createBodyMesh(CubeDeformation.NONE);
      PartDefinition root = mesh.getRoot();
      PartDefinition body = root.getChild("body");
      PartDefinition headParts = root.getChild("head_parts");
      body.addOrReplaceChild(
         "saddle", CubeListBuilder.create().texOffs(26, 0).addBox(-5.0F, -8.0F, -9.0F, 10.0F, 9.0F, 9.0F, new CubeDeformation(0.5F)), PartPose.ZERO
      );
      headParts.addOrReplaceChild("left_saddle_mouth", CubeListBuilder.create().texOffs(29, 5).addBox(2.0F, -9.0F, -6.0F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
      headParts.addOrReplaceChild("right_saddle_mouth", CubeListBuilder.create().texOffs(29, 5).addBox(-3.0F, -9.0F, -6.0F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
      headParts.addOrReplaceChild(
         "left_saddle_line",
         CubeListBuilder.create().texOffs(32, 2).addBox(3.1F, -6.0F, -8.0F, 0.0F, 3.0F, 16.0F),
         PartPose.rotation((float) (-Math.PI / 6), 0.0F, 0.0F)
      );
      headParts.addOrReplaceChild(
         "right_saddle_line",
         CubeListBuilder.create().texOffs(32, 2).addBox(-3.1F, -6.0F, -8.0F, 0.0F, 3.0F, 16.0F),
         PartPose.rotation((float) (-Math.PI / 6), 0.0F, 0.0F)
      );
      headParts.addOrReplaceChild(
         "head_saddle", CubeListBuilder.create().texOffs(1, 1).addBox(-3.0F, -11.0F, -1.9F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.22F)), PartPose.ZERO
      );
      headParts.addOrReplaceChild(
         "mouth_saddle_wrap", CubeListBuilder.create().texOffs(19, 0).addBox(-2.0F, -11.0F, -4.0F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.2F)), PartPose.ZERO
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   @Override
   public void setupAnim(final EquineRenderState state) {
      super.setupAnim(state);

      for (ModelPart part : this.ridingParts) {
         part.visible = state.isRidden;
      }
   }
}
