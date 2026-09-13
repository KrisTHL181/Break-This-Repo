package net.minecraft.client.model.object.skull;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.monster.piglin.PiglinModel;

public class PiglinHeadModel extends SkullModelBase {
   private final ModelPart head;
   private final ModelPart leftEar;
   private final ModelPart rightEar;

   public PiglinHeadModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");
      this.leftEar = this.head.getChild("left_ear");
      this.rightEar = this.head.getChild("right_ear");
   }

   public static MeshDefinition createHeadModel() {
      MeshDefinition mesh = new MeshDefinition();
      PiglinModel.addHead(CubeDeformation.NONE, mesh);
      return mesh;
   }

   public void setupAnim(final SkullModelBase.State state) {
      super.setupAnim(state);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      float asymmetry = 1.2F;
      this.leftEar.zRot = (float)(-(Math.cos(state.animationPos * (float) Math.PI * 0.2F * 1.2F) + 2.5)) * 0.2F;
      this.rightEar.zRot = (float)(Math.cos(state.animationPos * (float) Math.PI * 0.2F) + 2.5) * 0.2F;
   }
}
