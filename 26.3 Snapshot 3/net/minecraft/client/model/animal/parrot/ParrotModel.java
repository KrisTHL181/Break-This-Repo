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

public class ParrotModel extends EntityModel<ParrotRenderState> {
   private static final String FEATHER = "feather";
   private final ModelPart body;
   private final ModelPart tail;
   private final ModelPart leftWing;
   private final ModelPart rightWing;
   private final ModelPart head;
   private final ModelPart leftLeg;
   private final ModelPart rightLeg;

   public ParrotModel(final ModelPart root) {
      super(root);
      this.body = root.getChild("body");
      this.tail = root.getChild("tail");
      this.leftWing = root.getChild("left_wing");
      this.rightWing = root.getChild("right_wing");
      this.head = root.getChild("head");
      this.leftLeg = root.getChild("left_leg");
      this.rightLeg = root.getChild("right_leg");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(2, 8).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 16.5F, -3.0F, 0.4937F, 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(22, 1).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 4.0F, 1.0F),
         PartPose.offsetAndRotation(0.0F, 21.07F, 1.16F, 1.015F, 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().texOffs(19, 8).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F),
         PartPose.offsetAndRotation(1.5F, 16.94F, -2.76F, -0.6981F, (float) -Math.PI, 0.0F)
      );
      root.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().texOffs(19, 8).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F),
         PartPose.offsetAndRotation(-1.5F, 16.94F, -2.76F, -0.6981F, (float) -Math.PI, 0.0F)
      );
      PartDefinition head = root.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(2, 2).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 15.69F, -2.76F)
      );
      head.addOrReplaceChild(
         "head2", CubeListBuilder.create().texOffs(10, 0).addBox(-1.0F, -0.5F, -2.0F, 2.0F, 1.0F, 4.0F), PartPose.offset(0.0F, -2.0F, -1.0F)
      );
      head.addOrReplaceChild(
         "beak1", CubeListBuilder.create().texOffs(11, 7).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -0.5F, -1.5F)
      );
      head.addOrReplaceChild(
         "beak2", CubeListBuilder.create().texOffs(16, 7).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -1.75F, -2.45F)
      );
      head.addOrReplaceChild(
         "feather",
         CubeListBuilder.create().texOffs(2, 18).addBox(0.0F, -4.0F, -2.0F, 0.0F, 5.0F, 4.0F),
         PartPose.offsetAndRotation(0.0F, -2.15F, 0.15F, -0.2214F, 0.0F, 0.0F)
      );
      CubeListBuilder leg = CubeListBuilder.create().texOffs(14, 18).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
      root.addOrReplaceChild("left_leg", leg, PartPose.offsetAndRotation(1.0F, 22.0F, -1.05F, -0.0299F, 0.0F, 0.0F));
      root.addOrReplaceChild("right_leg", leg, PartPose.offsetAndRotation(-1.0F, 22.0F, -1.05F, -0.0299F, 0.0F, 0.0F));
      return LayerDefinition.create(mesh, 32, 32);
   }

   public void setupAnim(final ParrotRenderState state) {
      super.setupAnim(state);
      this.prepare(state.pose);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      switch (state.pose) {
         case STANDING:
            this.leftLeg.xRot = this.leftLeg.xRot + Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
            this.rightLeg.xRot = this.rightLeg.xRot + Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F * state.walkAnimationSpeed;
         case FLYING:
         case ON_SHOULDER:
         default:
            float bobbingBody = state.flapAngle * 0.3F;
            this.head.y += bobbingBody;
            this.tail.xRot = this.tail.xRot + Mth.cos(state.walkAnimationPos * 0.6662F) * 0.3F * state.walkAnimationSpeed;
            this.tail.y += bobbingBody;
            this.body.y += bobbingBody;
            this.leftWing.zRot = -0.0873F - state.flapAngle;
            this.leftWing.y += bobbingBody;
            this.rightWing.zRot = 0.0873F + state.flapAngle;
            this.rightWing.y += bobbingBody;
            this.leftLeg.y += bobbingBody;
            this.rightLeg.y += bobbingBody;
         case SITTING:
            break;
         case PARTY:
            float xPos = Mth.cos(state.ageInTicks);
            float yPos = Mth.sin(state.ageInTicks);
            this.head.x += xPos;
            this.head.y += yPos;
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.head.zRot = Mth.sin(state.ageInTicks) * 0.4F;
            this.body.x += xPos;
            this.body.y += yPos;
            this.leftWing.zRot = -0.0873F - state.flapAngle;
            this.leftWing.x += xPos;
            this.leftWing.y += yPos;
            this.rightWing.zRot = 0.0873F + state.flapAngle;
            this.rightWing.x += xPos;
            this.rightWing.y += yPos;
            this.tail.x += xPos;
            this.tail.y += yPos;
      }
   }

   private void prepare(final ParrotModel.Pose pose) {
      switch (pose) {
         case FLYING:
            this.leftLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
            this.rightLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
         case STANDING:
         case ON_SHOULDER:
         default:
            break;
         case SITTING:
            float sittingYOffset = 1.9F;
            this.head.y += 1.9F;
            this.tail.xRot += (float) (Math.PI / 6);
            this.tail.y += 1.9F;
            this.body.y += 1.9F;
            this.leftWing.zRot = -0.0873F;
            this.leftWing.y += 1.9F;
            this.rightWing.zRot = 0.0873F;
            this.rightWing.y += 1.9F;
            this.leftLeg.y += 1.9F;
            this.rightLeg.y += 1.9F;
            this.leftLeg.xRot += (float) (Math.PI / 2);
            this.rightLeg.xRot += (float) (Math.PI / 2);
            break;
         case PARTY:
            this.leftLeg.zRot = (float) (-Math.PI / 9);
            this.rightLeg.zRot = (float) (Math.PI / 9);
      }
   }

   public static ParrotModel.Pose getPose(final Parrot entity) {
      if (entity.isPartyParrot()) {
         return ParrotModel.Pose.PARTY;
      } else if (entity.isInSittingPose()) {
         return ParrotModel.Pose.SITTING;
      } else {
         return entity.isFlying() ? ParrotModel.Pose.FLYING : ParrotModel.Pose.STANDING;
      }
   }

   public enum Pose {
      FLYING,
      STANDING,
      SITTING,
      PARTY,
      ON_SHOULDER;
   }
}
