package net.minecraft.client.model.effects;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

public class SpinAttackEffectModel extends EntityModel<AvatarRenderState> {
   private static final int BOX_COUNT = 2;
   private final ModelPart[] boxes = new ModelPart[2];

   public SpinAttackEffectModel(final ModelPart root) {
      super(root);

      for (int i = 0; i < 2; i++) {
         this.boxes[i] = root.getChild(boxName(i));
      }
   }

   private static String boxName(final int i) {
      return "box" + i;
   }

   public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();

      for (int i = 0; i < 2; i++) {
         float yOffset = -3.2F + 9.6F * (i + 1);
         float scale = 0.75F * (i + 1);
         root.addOrReplaceChild(
            boxName(i), CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F + yOffset, -8.0F, 16.0F, 32.0F, 16.0F), PartPose.ZERO.withScale(scale)
         );
      }

      return LayerDefinition.create(mesh, 64, 64);
   }

   public void setupAnim(final AvatarRenderState state) {
      super.setupAnim(state);

      for (int i = 0; i < this.boxes.length; i++) {
         float angle = state.ageInTicks * -(45 + (i + 1) * 5);
         this.boxes[i].yRot = Mth.wrapDegrees(angle) * (float) (Math.PI / 180.0);
      }
   }
}
