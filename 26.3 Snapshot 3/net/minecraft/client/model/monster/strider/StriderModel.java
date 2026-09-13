package net.minecraft.client.model.monster.strider;

import java.util.function.BiConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.StriderRenderState;
import net.minecraft.util.Mth;

public abstract class StriderModel extends EntityModel<StriderRenderState> {
   protected static final float SPEED = 1.5F;
   protected final ModelPart rightLeg;
   protected final ModelPart leftLeg;
   protected final ModelPart body;

   public StriderModel(final ModelPart root) {
      super(root);
      this.rightLeg = root.getChild("right_leg");
      this.leftLeg = root.getChild("left_leg");
      this.body = root.getChild("body");
   }

   public void setupAnim(final StriderRenderState state) {
      super.setupAnim(state);
      float animationPos = state.walkAnimationPos;
      float animationSpeed = Math.min(state.walkAnimationSpeed, 0.25F);
      if (!state.isRidden) {
         this.body.xRot = state.xRot * (float) (Math.PI / 180.0);
         this.body.yRot = state.yRot * (float) (Math.PI / 180.0);
      } else {
         this.body.xRot = 0.0F;
         this.body.yRot = 0.0F;
      }

      this.body.zRot = 0.1F * Mth.sin(animationPos * 1.5F) * 4.0F * animationSpeed;
      this.leftLeg.xRot = Mth.sin(animationPos * 1.5F * 0.5F) * 2.0F * animationSpeed;
      this.rightLeg.xRot = Mth.sin(animationPos * 1.5F * 0.5F + (float) Math.PI) * 2.0F * animationSpeed;
      this.leftLeg.zRot = (float) (Math.PI / 18) * Mth.cos(animationPos * 1.5F * 0.5F) * animationSpeed;
      this.rightLeg.zRot = (float) (Math.PI / 18) * Mth.cos(animationPos * 1.5F * 0.5F + (float) Math.PI) * animationSpeed;
      this.customAnimations(animationPos, animationSpeed, state.ageInTicks);
   }

   protected abstract void customAnimations(final float animationPos, final float animationSpeed, final float ageInTicks);

   public void animateBristle(
      final float ageInTicks,
      final float bristleFlow,
      final ModelPart firstBristle,
      final ModelPart secondBristle,
      final ModelPart thirdBristle,
      final BiConsumer<ModelPart, Float> addRotationFunction
   ) {
      addRotationFunction.accept(firstBristle, bristleFlow * 0.6F);
      addRotationFunction.accept(secondBristle, bristleFlow * 1.2F);
      addRotationFunction.accept(thirdBristle, bristleFlow * 1.3F);
      addRotationFunction.accept(firstBristle, 0.1F * Mth.sin(ageInTicks * 0.4F));
      addRotationFunction.accept(secondBristle, 0.1F * Mth.sin(ageInTicks * 0.2F));
      addRotationFunction.accept(thirdBristle, 0.05F * Mth.sin(ageInTicks * -0.4F));
   }
}
