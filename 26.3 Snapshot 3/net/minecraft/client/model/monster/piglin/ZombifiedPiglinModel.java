package net.minecraft.client.model.monster.piglin;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;

public abstract class ZombifiedPiglinModel extends AbstractPiglinModel<ZombifiedPiglinRenderState> {
   public ZombifiedPiglinModel(final ModelPart root) {
      super(root);
   }

   public void setupAnim(final ZombifiedPiglinRenderState state) {
      super.setupAnim(state);
      AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, state.isAggressive, state);
   }
}
