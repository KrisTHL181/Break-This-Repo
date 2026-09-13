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

   public PlayerModel(final ModelPart root, final boolean slim) {
      super(root, RenderTypes::entityTranslucent);
      this.slim = slim;
      this.leftSleeve = this.leftArm.getChild("left_sleeve");
      this.rightSleeve = this.rightArm.getChild("right_sleeve");
      this.leftPants = this.leftLeg.getChild("left_pants");
      this.rightPants = this.rightLeg.getChild("right_pants");
      this.jacket = this.body.getChild("jacket");
      this.bodyParts = List.of(this.head, this.body, this.leftArm, this.rightArm, this.leftLeg, this.rightLeg);
   }

   public static MeshDefinition createMesh(final CubeDeformation scale, final boolean slim) {
      MeshDefinition mesh = HumanoidModel.createMesh(scale, 0.0F);
      PartDefinition root = mesh.getRoot();
      float overlayScale = 0.25F;
      if (slim) {
         PartDefinition leftArm = root.addOrReplaceChild(
            "left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, scale), PartPose.offset(5.0F, 2.0F, 0.0F)
         );
         PartDefinition rightArm = root.addOrReplaceChild(
            "right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, scale), PartPose.offset(-5.0F, 2.0F, 0.0F)
         );
         leftArm.addOrReplaceChild(
            "left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, scale.extend(0.25F)), PartPose.ZERO
         );
         rightArm.addOrReplaceChild(
            "right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, scale.extend(0.25F)), PartPose.ZERO
         );
      } else {
         PartDefinition leftArm = root.addOrReplaceChild(
            "left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, scale), PartPose.offset(5.0F, 2.0F, 0.0F)
         );
         PartDefinition rightArm = root.getChild("right_arm");
         leftArm.addOrReplaceChild(
            "left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, scale.extend(0.25F)), PartPose.ZERO
         );
         rightArm.addOrReplaceChild(
            "right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, scale.extend(0.25F)), PartPose.ZERO
         );
      }

      PartDefinition leftLeg = root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, scale), PartPose.offset(1.9F, 12.0F, 0.0F)
      );
      PartDefinition rightLeg = root.getChild("right_leg");
      leftLeg.addOrReplaceChild(
         "left_pants", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, scale.extend(0.25F)), PartPose.ZERO
      );
      rightLeg.addOrReplaceChild(
         "right_pants", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, scale.extend(0.25F)), PartPose.ZERO
      );
      PartDefinition body = root.getChild("body");
      body.addOrReplaceChild(
         "jacket", CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, scale.extend(0.25F)), PartPose.ZERO
      );
      return mesh;
   }

   public static ArmorModelSet<MeshDefinition> createArmorMeshSet(final CubeDeformation innerDeformation, final CubeDeformation outerDeformation) {
      return HumanoidModel.createArmorMeshSet(innerDeformation, outerDeformation).map(mesh -> {
         PartDefinition root = mesh.getRoot();
         PartDefinition leftArm = root.getChild("left_arm");
         PartDefinition rightArm = root.getChild("right_arm");
         leftArm.addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.ZERO);
         rightArm.addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.ZERO);
         PartDefinition leftLeg = root.getChild("left_leg");
         PartDefinition rightLeg = root.getChild("right_leg");
         leftLeg.addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.ZERO);
         rightLeg.addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.ZERO);
         PartDefinition body = root.getChild("body");
         body.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.ZERO);
         return (MeshDefinition)mesh;
      });
   }

   public void setupAnim(final AvatarRenderState state) {
      boolean showBody = !state.isSpectator;
      this.body.visible = showBody;
      this.rightArm.visible = showBody;
      this.leftArm.visible = showBody;
      this.rightLeg.visible = showBody;
      this.leftLeg.visible = showBody;
      this.hat.visible = state.showHat;
      this.jacket.visible = state.showJacket;
      this.leftPants.visible = state.showLeftPants;
      this.rightPants.visible = state.showRightPants;
      this.leftSleeve.visible = state.showLeftSleeve;
      this.rightSleeve.visible = state.showRightSleeve;
      super.setupAnim(state);
   }

   public void translateToHand(final AvatarRenderState state, final HumanoidArm arm, final PoseStack poseStack) {
      this.root().translateAndRotate(poseStack);
      ModelPart part = this.getArm(arm);
      if (this.slim) {
         float offset = 0.5F * (arm == HumanoidArm.RIGHT ? 1 : -1);
         part.x += offset;
         part.translateAndRotate(poseStack);
         part.x -= offset;
      } else {
         part.translateAndRotate(poseStack);
      }
   }

   public ModelPart getRandomBodyPart(final RandomSource random) {
      return Util.getRandom(this.bodyParts, random);
   }
}
