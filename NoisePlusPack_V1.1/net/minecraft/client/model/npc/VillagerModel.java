package net.minecraft.client.model.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillagerModel extends EntityModel<VillagerRenderState> implements HeadedModel, VillagerLikeModel<VillagerRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5F);
   private final ModelPart head;
   private final ModelPart rightLeg;
   private final ModelPart leftLeg;
   private final ModelPart arms;

   public VillagerModel(ModelPart p_456504_) {
      super(p_456504_);
      this.head = p_456504_.getChild("head");
      this.rightLeg = p_456504_.getChild("right_leg");
      this.leftLeg = p_456504_.getChild("left_leg");
      this.arms = p_456504_.getChild("arms");
   }

   public static MeshDefinition createBodyModel() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      float f = 0.5F;
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO
      );
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)), PartPose.ZERO
      );
      partdefinition2.addOrReplaceChild(
         "hat_rim",
         CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F),
         PartPose.rotation((float) (-Math.PI / 2), 0.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
         "nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -2.0F, 0.0F)
      );
      PartDefinition partdefinition3 = partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F), PartPose.ZERO
      );
      partdefinition3.addOrReplaceChild(
         "jacket", CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO
      );
      partdefinition.addOrReplaceChild(
         "arms",
         CubeListBuilder.create()
            .texOffs(44, 22)
            .addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
            .texOffs(44, 22)
            .addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, true)
            .texOffs(40, 38)
            .addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
         PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F)
      );
      return meshdefinition;
   }

   public static MeshDefinition createNoHatModel() {
      MeshDefinition meshdefinition = createBodyModel();
      meshdefinition.getRoot().clearChild("head").clearRecursively();
      return meshdefinition;
   }

   public void setupAnim(VillagerRenderState p_452536_) {
      super.setupAnim(p_452536_);
      this.head.yRot = p_452536_.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = p_452536_.xRot * (float) (Math.PI / 180.0);
      if (p_452536_.isUnhappy) {
         this.head.zRot = 0.3F * Mth.sin(0.45F * p_452536_.ageInTicks);
         this.head.xRot = 0.4F;
      } else {
         this.head.zRot = 0.0F;
      }

      this.rightLeg.xRot = Mth.cos(p_452536_.walkAnimationPos * 0.6662F) * 1.4F * p_452536_.walkAnimationSpeed * 0.5F;
      this.leftLeg.xRot = Mth.cos(p_452536_.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F * p_452536_.walkAnimationSpeed * 0.5F;
      this.rightLeg.yRot = 0.0F;
      this.leftLeg.yRot = 0.0F;
   }

   @Override
   public ModelPart getHead() {
      return this.head;
   }

   public void translateToArms(VillagerRenderState p_459938_, PoseStack p_450147_) {
      this.root.translateAndRotate(p_450147_);
      this.arms.translateAndRotate(p_450147_);
   }
}
