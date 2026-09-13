package net.minecraft.client.model.monster.piglin;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombifiedPiglinModel extends AbstractPiglinModel<ZombifiedPiglinRenderState> {
   public ZombifiedPiglinModel(ModelPart p_455893_) {
      super(p_455893_);
   }

   public void setupAnim(ZombifiedPiglinRenderState p_458040_) {
      super.setupAnim(p_458040_);
      AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, p_458040_.isAggressive, p_458040_);
   }

   @Override
   public void setAllVisible(boolean p_460933_) {
      super.setAllVisible(p_460933_);
      this.leftSleeve.visible = p_460933_;
      this.rightSleeve.visible = p_460933_;
      this.leftPants.visible = p_460933_;
      this.rightPants.visible = p_460933_;
      this.jacket.visible = p_460933_;
   }
}
