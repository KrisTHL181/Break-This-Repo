package net.minecraft.client.model.player;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerModel extends HumanoidModel<AvatarRenderState> {
   protected static final String LEFT_SLEEVE = "left_sleeve";
   protected static final String RIGHT_SLEEVE = "right_sleeve";
   protected static final String LEFT_PANTS = "left_pants";
   protected static final String RIGHT_PANTS = "right_pants";
   private final List<ModelPart> bodyParts;
   public final ModelPart leftSleeve;
   public final ModelPart rightSleeve;
   public final ModelPart leftPants;
   public final ModelPart rightPants;
   public final ModelPart jacket;
   private final boolean slim;

   public PlayerModel(ModelPart p_453451_, boolean p_451629_) {
      super(p_453451_, RenderTypes::entityTranslucent);
      this.slim = p_451629_;
      this.leftSleeve = this.leftArm.getChild("left_sleeve");
      this.rightSleeve = this.rightArm.getChild("right_sleeve");
      this.leftPants = this.leftLeg.getChild("left_pants");
      this.rightPants = this.rightLeg.getChild("right_pants");
      this.jacket = this.body.getChild("jacket");
      this.bodyParts = List.of(this.head, this.body, this.leftArm, this.rightArm, this.leftLeg, this.rightLeg);
   }

   public static MeshDefinition createMesh(CubeDeformation p_454085_, boolean p_455012_) {
      MeshDefinition meshdefinition = HumanoidModel.createMesh(p_454085_, 0.0F);
      PartDefinition partdefinition = meshdefinition.getRoot();
      float f = 0.25F;
      if (p_455012_) {
         PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
            "left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, p_454085_), PartPose.offset(5.0F, 2.0F, 0.0F)
         );
         PartDefinition partdefinition2 = partdefinition.addOrReplaceChild(
            "right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, p_454085_), PartPose.offset(-5.0F, 2.0F, 0.0F)
         );
         partdefinition1.addOrReplaceChild(
            "left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, p_454085_.extend(0.25F)), PartPose.ZERO
         );
         partdefinition2.addOrReplaceChild(
            "right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, p_454085_.extend(0.25F)), PartPose.ZERO
         );
      } else {
         PartDefinition partdefinition4 = partdefinition.addOrReplaceChild(
            "left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_454085_), PartPose.offset(5.0F, 2.0F, 0.0F)
         );
         PartDefinition partdefinition6 = partdefinition.getChild("right_arm");
         partdefinition4.addOrReplaceChild(
            "left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_454085_.extend(0.25F)), PartPose.ZERO
         );
         partdefinition6.addOrReplaceChild(
            "right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_454085_.extend(0.25F)), PartPose.ZERO
         );
      }

      PartDefinition partdefinition5 = partdefinition.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_454085_), PartPose.offset(1.9F, 12.0F, 0.0F)
      );
      PartDefinition partdefinition7 = partdefinition.getChild("right_leg");
      partdefinition5.addOrReplaceChild(
         "left_pants", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_454085_.extend(0.25F)), PartPose.ZERO
      );
      partdefinition7.addOrReplaceChild(
         "right_pants", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_454085_.extend(0.25F)), PartPose.ZERO
      );
      PartDefinition partdefinition3 = partdefinition.getChild("body");
      partdefinition3.addOrReplaceChild(
         "jacket", CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, p_454085_.extend(0.25F)), PartPose.ZERO
      );
      return meshdefinition;
   }

   public static ArmorModelSet<MeshDefinition> createArmorMeshSet(CubeDeformation p_454036_, CubeDeformation p_454822_) {
      return HumanoidModel.createArmorMeshSet(p_454036_, p_454822_).map(p_459328_ -> {
         PartDefinition partdefinition = p_459328_.getRoot();
         PartDefinition partdefinition1 = partdefinition.getChild("left_arm");
         PartDefinition partdefinition2 = partdefinition.getChild("right_arm");
         partdefinition1.addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.ZERO);
         partdefinition2.addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.ZERO);
         PartDefinition partdefinition3 = partdefinition.getChild("left_leg");
         PartDefinition partdefinition4 = partdefinition.getChild("right_leg");
         partdefinition3.addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.ZERO);
         partdefinition4.addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.ZERO);
         PartDefinition partdefinition5 = partdefinition.getChild("body");
         partdefinition5.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.ZERO);
         return (MeshDefinition)p_459328_;
      });
   }

   public void setupAnim(AvatarRenderState p_451323_) {
      boolean flag = !p_451323_.isSpectator;
      this.body.visible = flag;
      this.rightArm.visible = flag;
      this.leftArm.visible = flag;
      this.rightLeg.visible = flag;
      this.leftLeg.visible = flag;
      this.hat.visible = p_451323_.showHat;
      this.jacket.visible = p_451323_.showJacket;
      this.leftPants.visible = p_451323_.showLeftPants;
      this.rightPants.visible = p_451323_.showRightPants;
      this.leftSleeve.visible = p_451323_.showLeftSleeve;
      this.rightSleeve.visible = p_451323_.showRightSleeve;
      super.setupAnim(p_451323_);
   }

   @Override
   public void setAllVisible(boolean p_457979_) {
      super.setAllVisible(p_457979_);
      this.leftSleeve.visible = p_457979_;
      this.rightSleeve.visible = p_457979_;
      this.leftPants.visible = p_457979_;
      this.rightPants.visible = p_457979_;
      this.jacket.visible = p_457979_;
   }

   public void translateToHand(AvatarRenderState p_458021_, HumanoidArm p_450801_, PoseStack p_452632_) {
      this.root().translateAndRotate(p_452632_);
      ModelPart modelpart = this.getArm(p_450801_);
      if (this.slim) {
         float f = 0.5F * (p_450801_ == HumanoidArm.RIGHT ? 1 : -1);
         modelpart.x += f;
         modelpart.translateAndRotate(p_452632_);
         modelpart.x -= f;
      } else {
         modelpart.translateAndRotate(p_452632_);
      }
   }

   public ModelPart getRandomBodyPart(RandomSource p_456405_) {
      return Util.getRandom(this.bodyParts, p_456405_);
   }
}
