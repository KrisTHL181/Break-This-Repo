package net.minecraft.client.model.animal.polarbear;

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
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PolarBearModel extends QuadrupedModel<PolarBearRenderState> {
   private static final float BABY_HEAD_SCALE = 2.25F;
   private static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 16.0F, 4.0F, 2.25F, 2.0F, 24.0F, Set.of("head"));

   public PolarBearModel(ModelPart p_457743_) {
      super(p_457743_);
   }

   public static LayerDefinition createBodyLayer(boolean p_452344_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-3.5F, -3.0F, -3.0F, 7.0F, 7.0F, 7.0F)
            .texOffs(0, 44)
            .addBox("mouth", -2.5F, 1.0F, -6.0F, 5.0F, 3.0F, 3.0F)
            .texOffs(26, 0)
            .addBox("right_ear", -4.5F, -4.0F, -1.0F, 2.0F, 2.0F, 1.0F)
            .texOffs(26, 0)
            .mirror()
            .addBox("left_ear", 2.5F, -4.0F, -1.0F, 2.0F, 2.0F, 1.0F),
         PartPose.offset(0.0F, 10.0F, -16.0F)
      );
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 19)
            .addBox(-5.0F, -13.0F, -7.0F, 14.0F, 14.0F, 11.0F)
            .texOffs(39, 0)
            .addBox(-4.0F, -25.0F, -7.0F, 12.0F, 12.0F, 10.0F),
         PartPose.offsetAndRotation(-2.0F, 9.0F, 12.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      int i = 10;
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(50, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 8.0F);
      partdefinition.addOrReplaceChild("right_hind_leg", cubelistbuilder, PartPose.offset(-4.5F, 14.0F, 6.0F));
      partdefinition.addOrReplaceChild("left_hind_leg", cubelistbuilder, PartPose.offset(4.5F, 14.0F, 6.0F));
      CubeListBuilder cubelistbuilder1 = CubeListBuilder.create().texOffs(50, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 6.0F);
      partdefinition.addOrReplaceChild("right_front_leg", cubelistbuilder1, PartPose.offset(-3.5F, 14.0F, -8.0F));
      partdefinition.addOrReplaceChild("left_front_leg", cubelistbuilder1, PartPose.offset(3.5F, 14.0F, -8.0F));
      return LayerDefinition.create(meshdefinition, 128, 64)
         .apply(p_452344_ ? BABY_TRANSFORMER : MeshTransformer.IDENTITY)
         .apply(MeshTransformer.scaling(1.2F));
   }

   public void setupAnim(PolarBearRenderState p_458684_) {
      super.setupAnim(p_458684_);
      float f = p_458684_.standScale * p_458684_.standScale;
      float f1 = p_458684_.ageScale;
      float f2 = p_458684_.isBaby ? 0.44444445F : 1.0F;
      this.body.xRot -= f * (float) Math.PI * 0.35F;
      this.body.y += f * f1 * 2.0F;
      this.rightFrontLeg.y -= f * f1 * 20.0F;
      this.rightFrontLeg.z += f * f1 * 4.0F;
      this.rightFrontLeg.xRot -= f * (float) Math.PI * 0.45F;
      this.leftFrontLeg.y = this.rightFrontLeg.y;
      this.leftFrontLeg.z = this.rightFrontLeg.z;
      this.leftFrontLeg.xRot -= f * (float) Math.PI * 0.45F;
      this.head.y -= f * f2 * 24.0F;
      this.head.z += f * f2 * 13.0F;
      this.head.xRot += f * (float) Math.PI * 0.15F;
   }
}
