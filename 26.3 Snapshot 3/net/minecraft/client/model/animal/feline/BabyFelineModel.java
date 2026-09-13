package net.minecraft.client.model.animal.feline;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.util.Mth;

public class BabyFelineModel<S extends FelineRenderState> extends AbstractFelineModel<S> {
   protected BabyFelineModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBabyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-2.5F, -3.0F, -2.875F, 5.0F, 4.0F, 4.0F)
            .texOffs(18, 0)
            .addBox(-2.0F, -4.0F, -0.875F, 1.0F, 1.0F, 2.0F)
            .texOffs(24, 0)
            .addBox(1.0F, -4.0F, -0.875F, 1.0F, 1.0F, 2.0F)
            .texOffs(18, 3)
            .addBox(-1.5F, -1.0F, -3.875F, 3.0F, 2.0F, 1.0F),
         PartPose.offset(0.0F, 20.0F, -3.125F)
      );
      partdefinition.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(18, 18).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(1.0F, 22.0F, -1.5F)
      );
      partdefinition.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(12, 18).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(-1.0F, 22.0F, -1.5F)
      );
      partdefinition.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(18, 22).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(1.0F, 22.0F, 2.5F)
      );
      partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0F, -1.5F, -3.5F, 4.0F, 3.0F, 7.0F), PartPose.offset(0.0F, 20.5F, 0.5F)
      );
      partdefinition.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(12, 22).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(-1.0F, 22.0F, 2.5F)
      );
      partdefinition.addOrReplaceChild(
         "tail1",
         CubeListBuilder.create().texOffs(0, 18).addBox(-0.5F, -0.107F, 0.0849F, 1.0F, 1.0F, 5.0F),
         PartPose.offsetAndRotation(0.0F, 19.107F, 3.9151F, -0.567232F, 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(final S state) {
      super.setupAnim(state);
      float ageScale = state.ageScale;
      if (state.isCrouching) {
         this.body.y += 1.0F * ageScale;
         this.head.y += 2.0F * ageScale;
         this.tail1.y += 1.0F * ageScale;
         this.tail2.y += -4.0F * ageScale;
         this.tail2.z += 2.0F * ageScale;
         this.tail1.xRot = (float) (Math.PI / 2);
         this.tail2.xRot = (float) (Math.PI / 2);
      } else if (state.isSprinting) {
         this.tail2.y = this.tail1.y;
         this.tail2.z += 2.0F * ageScale;
         this.tail1.xRot = (float) (Math.PI / 2);
         this.tail2.xRot = (float) (Math.PI / 2);
      }

      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      if (!state.isSitting) {
         float animationSpeed = state.walkAnimationSpeed;
         float animationPos = state.walkAnimationPos;
         if (state.isSprinting) {
            this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662F) * animationSpeed;
            this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662F + 0.3F) * animationSpeed;
            this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI + 0.3F) * animationSpeed;
            this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * animationSpeed;
            this.tail2.xRot = 1.7278761F + (float) (Math.PI / 10) * Mth.cos(animationPos) * animationSpeed;
         } else {
            this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662F) * animationSpeed;
            this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * animationSpeed;
            this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * animationSpeed;
            this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662F) * animationSpeed;
            if (!state.isCrouching) {
               this.tail2.xRot = 1.7278761F + (float) (Math.PI / 4) * Mth.cos(animationPos) * animationSpeed;
            } else {
               this.tail2.xRot = 1.7278761F + 0.47123894F * Mth.cos(animationPos) * animationSpeed;
            }
         }
      } else if (state.isSitting) {
         this.body.xRot += -0.43633232F;
         this.body.y += 1.25F;
         this.head.z += 0.75F;
         this.tail1.xRot += 0.5454154F;
         this.tail1.y += 4.0F;
         this.tail1.z -= 0.9F;
         this.leftHindLeg.z -= 0.9F;
         this.rightHindLeg.z -= 0.9F;
      }

      if (state.lieDownAmount > 0.0F) {
         this.body.x++;
         this.head.xRot = Mth.rotLerp(state.lieDownAmount, this.head.xRot, (float) (Math.PI / 18));
         this.head.zRot = Mth.rotLerp(state.lieDownAmount, this.head.zRot, (float) (-Math.PI * 5.0 / 12.0));
         this.head.x += 1.5F;
         this.head.y += 0.75F;
         this.head.z -= 0.5F;
         this.rightFrontLeg.xRot = (float) (-Math.PI / 4);
         this.rightFrontLeg.x += 3.5F;
         this.rightFrontLeg.y -= 0.5F;
         this.rightFrontLeg.z += 0.0F;
         this.leftFrontLeg.xRot = (float) (-Math.PI / 2);
         this.leftFrontLeg.x += 1.5F;
         this.leftFrontLeg.y--;
         this.leftFrontLeg.z -= 2.0F;
         this.rightHindLeg.xRot = (float) (Math.PI * 2.0 / 9.0);
         this.rightHindLeg.yRot = (float) (Math.PI / 9);
         this.rightHindLeg.zRot = (float) (-Math.PI / 9);
         this.rightHindLeg.x += 2.5F;
         this.rightHindLeg.y -= 0.25F;
         this.rightHindLeg.z += 0.5F;
         this.leftHindLeg.x += 1.5F;
         this.leftHindLeg.z--;
         this.tail1.xRot = this.tail1.xRot + Mth.rotLerp(state.lieDownAmountTail, this.tail1.xRot, (float) (-Math.PI / 6));
         this.tail1.yRot = this.tail1.yRot + Mth.rotLerp(state.lieDownAmountTail, this.tail1.yRot, 0.0F);
         this.tail1.zRot = this.tail1.zRot + Mth.rotLerp(state.lieDownAmountTail, this.tail1.zRot, (float) (-Math.PI / 18));
         this.tail1.x++;
         this.tail1.y += 0.5F;
         this.tail1.z -= 0.25F;
      }

      if (state.relaxStateOneAmount > 0.0F) {
         this.head.xRot = Mth.rotLerp(state.relaxStateOneAmount, this.head.xRot, -0.58177644F);
      }
   }
}
