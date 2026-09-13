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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpinAttackEffectModel extends EntityModel<AvatarRenderState> {
   private static final int BOX_COUNT = 2;
   private final ModelPart[] boxes = new ModelPart[2];

   public SpinAttackEffectModel(ModelPart p_452209_) {
      super(p_452209_);

      for (int i = 0; i < 2; i++) {
         this.boxes[i] = p_452209_.getChild(boxName(i));
      }
   }

   private static String boxName(int p_451157_) {
      return "box" + p_451157_;
   }

   public static LayerDefinition createLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();

      for (int i = 0; i < 2; i++) {
         float f = -3.2F + 9.6F * (i + 1);
         float f1 = 0.75F * (i + 1);
         partdefinition.addOrReplaceChild(
            boxName(i), CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F + f, -8.0F, 16.0F, 32.0F, 16.0F), PartPose.ZERO.withScale(f1)
         );
      }

      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(AvatarRenderState p_450298_) {
      super.setupAnim(p_450298_);

      for (int i = 0; i < this.boxes.length; i++) {
         float f = p_450298_.ageInTicks * -(45 + (i + 1) * 5);
         this.boxes[i].yRot = Mth.wrapDegrees(f) * (float) (Math.PI / 180.0);
      }
   }
}
