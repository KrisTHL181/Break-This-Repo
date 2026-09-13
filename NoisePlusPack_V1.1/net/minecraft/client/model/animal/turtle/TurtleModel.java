package net.minecraft.client.model.animal.turtle;

import java.util.Set;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TurtleModel extends QuadrupedModel<TurtleRenderState> {
   private static final String EGG_BELLY = "egg_belly";
   public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 120.0F, 0.0F, 9.0F, 6.0F, 120.0F, Set.of("head"));
   private final ModelPart eggBelly;

   public TurtleModel(ModelPart p_451359_) {
      super(p_451359_);
      this.eggBelly = p_451359_.getChild("egg_belly");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(3, 0).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 5.0F, 6.0F), PartPose.offset(0.0F, 19.0F, -10.0F)
      );
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(7, 37)
            .addBox("shell", -9.5F, 3.0F, -10.0F, 19.0F, 20.0F, 6.0F)
            .texOffs(31, 1)
            .addBox("belly", -5.5F, 3.0F, -13.0F, 11.0F, 18.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 11.0F, -10.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "egg_belly",
         CubeListBuilder.create().texOffs(70, 33).addBox(-4.5F, 3.0F, -14.0F, 9.0F, 18.0F, 1.0F),
         PartPose.offsetAndRotation(0.0F, 11.0F, -10.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      int i = 1;
      partdefinition.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(1, 23).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 10.0F), PartPose.offset(-3.5F, 22.0F, 11.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(1, 12).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 10.0F), PartPose.offset(3.5F, 22.0F, 11.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(27, 30).addBox(-13.0F, 0.0F, -2.0F, 13.0F, 1.0F, 5.0F), PartPose.offset(-5.0F, 21.0F, -4.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(27, 24).addBox(0.0F, 0.0F, -2.0F, 13.0F, 1.0F, 5.0F), PartPose.offset(5.0F, 21.0F, -4.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 64);
   }

   public void setupAnim(TurtleRenderState p_455607_) {
      super.setupAnim(p_455607_);
      float f = p_455607_.walkAnimationPos;
      float f1 = p_455607_.walkAnimationSpeed;
      if (p_455607_.isOnLand) {
         float f2 = p_455607_.isLayingEgg ? 4.0F : 1.0F;
         float f3 = p_455607_.isLayingEgg ? 2.0F : 1.0F;
         float f4 = f * 5.0F;
         float f5 = Mth.cos(f2 * f4);
         float f6 = Mth.cos(f4);
         this.rightFrontLeg.yRot = -f5 * 8.0F * f1 * f3;
         this.leftFrontLeg.yRot = f5 * 8.0F * f1 * f3;
         this.rightHindLeg.yRot = -f6 * 3.0F * f1;
         this.leftHindLeg.yRot = f6 * 3.0F * f1;
      } else {
         float f7 = 0.5F * f1;
         float f8 = Mth.cos(f * 0.6662F * 0.6F) * f7;
         this.rightHindLeg.xRot = f8;
         this.leftHindLeg.xRot = -f8;
         this.rightFrontLeg.zRot = -f8;
         this.leftFrontLeg.zRot = f8;
      }

      this.eggBelly.visible = p_455607_.hasEgg;
      if (this.eggBelly.visible) {
         this.root.y--;
      }
   }
}
