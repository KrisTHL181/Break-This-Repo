package net.minecraft.client.model.animal.equine;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;

public class DonkeyModel extends AbstractEquineModel<DonkeyRenderState> {
   public static final float DONKEY_SCALE = 0.87F;
   public static final float MULE_SCALE = 0.92F;
   private static final MeshTransformer DONKEY_TRANSFORMER = mesh -> {
      modifyMesh(mesh.getRoot());
      return mesh;
   };
   private final ModelPart leftChest = this.body.getChild("left_chest");
   private final ModelPart rightChest = this.body.getChild("right_chest");

   public DonkeyModel(final ModelPart root) {
      super(root);
   }

   public DonkeyModel(
      final ModelPart root,
      final ModelPart headParts,
      final ModelPart rightHindLeg,
      final ModelPart rightFrontLeg,
      final ModelPart leftHindLeg,
      final ModelPart leftFrontLeg,
      final ModelPart tail
   ) {
      super(root, headParts, rightHindLeg, rightFrontLeg, leftHindLeg, leftFrontLeg, tail);
   }

   public static LayerDefinition createBodyLayer(final float scale) {
      return LayerDefinition.create(AbstractEquineModel.createBodyMesh(CubeDeformation.NONE), 64, 64)
         .apply(DONKEY_TRANSFORMER)
         .apply(MeshTransformer.scaling(scale));
   }

   public static LayerDefinition createSaddleLayer(final float scale) {
      return EquineSaddleModel.createSaddleLayer().apply(DONKEY_TRANSFORMER).apply(MeshTransformer.scaling(scale));
   }

   private static void modifyMesh(final PartDefinition root) {
      PartDefinition body = root.getChild("body");
      CubeListBuilder chest = CubeListBuilder.create().texOffs(26, 21).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 3.0F);
      body.addOrReplaceChild("left_chest", chest, PartPose.offsetAndRotation(6.0F, -8.0F, 0.0F, 0.0F, (float) (-Math.PI / 2), 0.0F));
      body.addOrReplaceChild("right_chest", chest, PartPose.offsetAndRotation(-6.0F, -8.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F));
      PartDefinition head = root.getChild("head_parts").getChild("head");
      CubeListBuilder ear = CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, -7.0F, 0.0F, 2.0F, 7.0F, 1.0F);
      head.addOrReplaceChild("left_ear", ear, PartPose.offsetAndRotation(1.25F, -10.0F, 4.0F, (float) (Math.PI / 12), 0.0F, (float) (Math.PI / 12)));
      head.addOrReplaceChild("right_ear", ear, PartPose.offsetAndRotation(-1.25F, -10.0F, 4.0F, (float) (Math.PI / 12), 0.0F, (float) (-Math.PI / 12)));
   }

   public void setupAnim(final DonkeyRenderState state) {
      super.setupAnim(state);
      this.leftChest.visible = state.hasChest;
      this.rightChest.visible = state.hasChest;
   }
}
