package net.minecraft.client.model.animal.parrot;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParrotModel extends EntityModel<ParrotRenderState> {
   private static final String FEATHER = "feather";
   private final ModelPart body;
   private final ModelPart tail;
   private final ModelPart leftWing;
   private final ModelPart rightWing;
   private final ModelPart head;
   private final ModelPart leftLeg;
   private final ModelPart rightLeg;

   public ParrotModel(ModelPart p_457449_) {
      super(p_457449_);
      this.body = p_457449_.getChild("body");
      this.tail = p_457449_.getChild("tail");
      this.leftWing = p_457449_.getChild("left_wing");
      this.rightWing = p_457449_.getChild("right_wing");
      this.head = p_457449_.getChild("head");
      this.leftLeg = p_457449_.getChild("left_leg");
      this.rightLeg = p_457449_.getChild("right_leg");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(2, 8).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 16.5F, -3.0F, 0.4937F, 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(22, 1).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 4.0F, 1.0F),
         PartPose.offsetAndRotation(0.0F, 21.07F, 1.16F, 1.015F, 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().texOffs(19, 8).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F),
         PartPose.offsetAndRotation(1.5F, 16.94F, -2.76F, -0.6981F, (float) -Math.PI, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().texOffs(19, 8).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F),
         PartPose.offsetAndRotation(-1.5F, 16.94F, -2.76F, -0.6981F, (float) -Math.PI, 0.0F)
      );
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(2, 2).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 15.69F, -2.76F)
      );
      partdefinition1.addOrReplaceChild(
         "head2", CubeListBuilder.create().texOffs(10, 0).addBox(-1.0F, -0.5F, -2.0F, 2.0F, 1.0F, 4.0F), PartPose.offset(0.0F, -2.0F, -1.0F)
      );
      partdefinition1.addOrReplaceChild(
         "beak1", CubeListBuilder.create().texOffs(11, 7).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -0.5F, -1.5F)
      );
      partdefinition1.addOrReplaceChild(
         "beak2", CubeListBuilder.create().texOffs(16, 7).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -1.75F, -2.45F)
      );
      partdefinition1.addOrReplaceChild(
         "feather",
         CubeListBuilder.create().texOffs(2, 18).addBox(0.0F, -4.0F, -2.0F, 0.0F, 5.0F, 4.0F),
         PartPose.offsetAndRotation(0.0F, -2.15F, 0.15F, -0.2214F, 0.0F, 0.0F)
      );
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(14, 18).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
      partdefinition.addOrReplaceChild("left_leg", cubelistbuilder, PartPose.offsetAndRotation(1.0F, 22.0F, -1.05F, -0.0299F, 0.0F, 0.0F));
      partdefinition.addOrReplaceChild("right_leg", cubelistbuilder, PartPose.offsetAndRotation(-1.0F, 22.0F, -1.05F, -0.0299F, 0.0F, 0.0F));
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(ParrotRenderState p_456499_) {
      super.setupAnim(p_456499_);
      this.prepare(p_456499_.pose);
      this.head.xRot = p_456499_.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = p_456499_.yRot * (float) (Math.PI / 180.0);
      switch (p_456499_.pose) {
         case STANDING:
            this.leftLeg.xRot = this.leftLeg.xRot + Mth.cos(p_456499_.walkAnimationPos * 0.6662F) * 1.4F * p_456499_.walkAnimationSpeed;
            this.rightLeg.xRot = this.rightLeg.xRot + Mth.cos(p_456499_.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F * p_456499_.walkAnimationSpeed;
         case FLYING:
         case ON_SHOULDER:
         default:
            float f2 = p_456499_.flapAngle * 0.3F;
            this.head.y += f2;
            this.tail.xRot = this.tail.xRot + Mth.cos(p_456499_.walkAnimationPos * 0.6662F) * 0.3F * p_456499_.walkAnimationSpeed;
            this.tail.y += f2;
            this.body.y += f2;
            this.leftWing.zRot = -0.0873F - p_456499_.flapAngle;
            this.leftWing.y += f2;
            this.rightWing.zRot = 0.0873F + p_456499_.flapAngle;
            this.rightWing.y += f2;
            this.leftLeg.y += f2;
            this.rightLeg.y += f2;
         case SITTING:
            break;
         case PARTY:
            float f = Mth.cos(p_456499_.ageInTicks);
            float f1 = Mth.sin(p_456499_.ageInTicks);
            this.head.x += f;
            this.head.y += f1;
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.head.zRot = Mth.sin(p_456499_.ageInTicks) * 0.4F;
            this.body.x += f;
            this.body.y += f1;
            this.leftWing.zRot = -0.0873F - p_456499_.flapAngle;
            this.leftWing.x += f;
            this.leftWing.y += f1;
            this.rightWing.zRot = 0.0873F + p_456499_.flapAngle;
            this.rightWing.x += f;
            this.rightWing.y += f1;
            this.tail.x += f;
            this.tail.y += f1;
      }
   }

   private void prepare(ParrotModel.Pose p_455298_) {
      switch (p_455298_) {
         case FLYING:
            this.leftLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
            this.rightLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
         case STANDING:
         case ON_SHOULDER:
         default:
            break;
         case SITTING:
            float f = 1.9F;
            this.head.y++;
            this.tail.xRot += (float) (Math.PI / 6);
            this.tail.y++;
            this.body.y++;
            this.leftWing.zRot = -0.0873F;
            this.leftWing.y++;
            this.rightWing.zRot = 0.0873F;
            this.rightWing.y++;
            this.leftLeg.y++;
            this.rightLeg.y++;
            this.leftLeg.xRot++;
            this.rightLeg.xRot++;
            break;
         case PARTY:
            this.leftLeg.zRot = (float) (-Math.PI / 9);
            this.rightLeg.zRot = (float) (Math.PI / 9);
      }
   }

   public static ParrotModel.Pose getPose(Parrot p_450178_) {
      if (p_450178_.isPartyParrot()) {
         return ParrotModel.Pose.PARTY;
      } else if (p_450178_.isInSittingPose()) {
         return ParrotModel.Pose.SITTING;
      } else {
         return p_450178_.isFlying() ? ParrotModel.Pose.FLYING : ParrotModel.Pose.STANDING;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum Pose {
      FLYING,
      STANDING,
      SITTING,
      PARTY,
      ON_SHOULDER;
   }
}
