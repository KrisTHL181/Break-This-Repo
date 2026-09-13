package net.minecraft.client.model.object.skull;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.monster.piglin.PiglinModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PiglinHeadModel extends SkullModelBase {
   private final ModelPart head;
   private final ModelPart leftEar;
   private final ModelPart rightEar;

   public PiglinHeadModel(ModelPart p_452606_) {
      super(p_452606_);
      this.head = p_452606_.getChild("head");
      this.leftEar = this.head.getChild("left_ear");
      this.rightEar = this.head.getChild("right_ear");
   }

   public static MeshDefinition createHeadModel() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PiglinModel.addHead(CubeDeformation.NONE, meshdefinition);
      return meshdefinition;
   }

   public void setupAnim(SkullModelBase.State p_453997_) {
      super.setupAnim(p_453997_);
      this.head.yRot = p_453997_.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = p_453997_.xRot * (float) (Math.PI / 180.0);
      float f = 1.2F;
      this.leftEar.zRot = (float)(-(Math.cos(p_453997_.animationPos * (float) Math.PI * 0.2F * 1.2F) + 2.5)) * 0.2F;
      this.rightEar.zRot = (float)(Math.cos(p_453997_.animationPos * (float) Math.PI * 0.2F) + 2.5) * 0.2F;
   }
}
