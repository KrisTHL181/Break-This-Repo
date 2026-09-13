package net.minecraft.client.model.monster.hoglin;

import java.util.Set;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HoglinModel extends EntityModel<HoglinRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 8.0F, 6.0F, 1.9F, 2.0F, 24.0F, Set.of("head"));
   private static final float DEFAULT_HEAD_X_ROT = 0.87266463F;
   private static final float ATTACK_HEAD_X_ROT_END = (float) (-Math.PI / 9);
   private final ModelPart head;
   private final ModelPart rightEar;
   private final ModelPart leftEar;
   private final ModelPart body;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart mane;

   public HoglinModel(ModelPart p_451817_) {
      super(p_451817_);
      this.body = p_451817_.getChild("body");
      this.mane = this.body.getChild("mane");
      this.head = p_451817_.getChild("head");
      this.rightEar = this.head.getChild("right_ear");
      this.leftEar = this.head.getChild("left_ear");
      this.rightFrontLeg = p_451817_.getChild("right_front_leg");
      this.leftFrontLeg = p_451817_.getChild("left_front_leg");
      this.rightHindLeg = p_451817_.getChild("right_hind_leg");
      this.leftHindLeg = p_451817_.getChild("left_hind_leg");
   }

   private static MeshDefinition createMesh() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(1, 1).addBox(-8.0F, -7.0F, -13.0F, 16.0F, 14.0F, 26.0F), PartPose.offset(0.0F, 7.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
         "mane",
         CubeListBuilder.create().texOffs(90, 33).addBox(0.0F, 0.0F, -9.0F, 0.0F, 10.0F, 19.0F, new CubeDeformation(0.001F)),
         PartPose.offset(0.0F, -14.0F, -7.0F)
      );
      PartDefinition partdefinition2 = partdefinition.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(61, 1).addBox(-7.0F, -3.0F, -19.0F, 14.0F, 6.0F, 19.0F),
         PartPose.offsetAndRotation(0.0F, 2.0F, -12.0F, 0.87266463F, 0.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_ear",
         CubeListBuilder.create().texOffs(1, 1).addBox(-6.0F, -1.0F, -2.0F, 6.0F, 1.0F, 4.0F),
         PartPose.offsetAndRotation(-6.0F, -2.0F, -3.0F, 0.0F, 0.0F, (float) (-Math.PI * 2.0 / 9.0))
      );
      partdefinition2.addOrReplaceChild(
         "left_ear",
         CubeListBuilder.create().texOffs(1, 6).addBox(0.0F, -1.0F, -2.0F, 6.0F, 1.0F, 4.0F),
         PartPose.offsetAndRotation(6.0F, -2.0F, -3.0F, 0.0F, 0.0F, (float) (Math.PI * 2.0 / 9.0))
      );
      partdefinition2.addOrReplaceChild(
         "right_horn", CubeListBuilder.create().texOffs(10, 13).addBox(-1.0F, -11.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offset(-7.0F, 2.0F, -12.0F)
      );
      partdefinition2.addOrReplaceChild(
         "left_horn", CubeListBuilder.create().texOffs(1, 13).addBox(-1.0F, -11.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offset(7.0F, 2.0F, -12.0F)
      );
      int i = 14;
      int j = 11;
      partdefinition.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(66, 42).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F), PartPose.offset(-4.0F, 10.0F, -8.5F)
      );
      partdefinition.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(41, 42).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F), PartPose.offset(4.0F, 10.0F, -8.5F)
      );
      partdefinition.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(21, 45).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F), PartPose.offset(-5.0F, 13.0F, 10.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(0, 45).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F), PartPose.offset(5.0F, 13.0F, 10.0F)
      );
      return meshdefinition;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = createMesh();
      return LayerDefinition.create(meshdefinition, 128, 64);
   }

   public static LayerDefinition createBabyLayer() {
      MeshDefinition meshdefinition = createMesh();
      PartDefinition partdefinition = meshdefinition.getRoot().getChild("body");
      partdefinition.addOrReplaceChild(
         "mane",
         CubeListBuilder.create().texOffs(90, 33).addBox(0.0F, 0.0F, -9.0F, 0.0F, 10.0F, 19.0F, new CubeDeformation(0.001F)),
         PartPose.offset(0.0F, -14.0F, -3.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 64).apply(BABY_TRANSFORMER);
   }

   public void setupAnim(HoglinRenderState p_457777_) {
      super.setupAnim(p_457777_);
      float f = p_457777_.walkAnimationSpeed;
      float f1 = p_457777_.walkAnimationPos;
      this.rightEar.zRot = (float) (-Math.PI * 2.0 / 9.0) - f * Mth.sin(f1);
      this.leftEar.zRot = (float) (Math.PI * 2.0 / 9.0) + f * Mth.sin(f1);
      this.head.yRot = p_457777_.yRot * (float) (Math.PI / 180.0);
      float f2 = 1.0F - Mth.abs(10 - 2 * p_457777_.attackAnimationRemainingTicks) / 10.0F;
      this.head.xRot = Mth.lerp(f2, 0.87266463F, (float) (-Math.PI / 9));
      if (p_457777_.isBaby) {
         this.head.y += f2 * 2.5F;
      }

      float f3 = 1.2F;
      this.rightFrontLeg.xRot = Mth.cos(f1) * 1.2F * f;
      this.leftFrontLeg.xRot = Mth.cos(f1 + (float) Math.PI) * 1.2F * f;
      this.rightHindLeg.xRot = this.leftFrontLeg.xRot;
      this.leftHindLeg.xRot = this.rightFrontLeg.xRot;
   }
}
