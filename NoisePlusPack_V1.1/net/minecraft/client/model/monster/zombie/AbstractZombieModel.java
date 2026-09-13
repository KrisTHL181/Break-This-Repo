package net.minecraft.client.model.monster.zombie;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractZombieModel<S extends ZombieRenderState> extends HumanoidModel<S> {
   protected AbstractZombieModel(ModelPart p_455883_) {
      super(p_455883_);
   }

   public void setupAnim(S p_456215_) {
      super.setupAnim(p_456215_);
      AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, p_456215_.isAggressive, p_456215_);
   }
}
