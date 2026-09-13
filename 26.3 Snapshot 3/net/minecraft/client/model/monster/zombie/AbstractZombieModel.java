package net.minecraft.client.model.monster.zombie;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;

public abstract class AbstractZombieModel<S extends ZombieRenderState> extends HumanoidModel<S> {
   protected AbstractZombieModel(final ModelPart root) {
      super(root);
   }

   public void setupAnim(final S state) {
      super.setupAnim(state);
      AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, state.isAggressive, state);
   }
}
