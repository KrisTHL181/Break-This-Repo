package net.minecraft.client.model.object.boat;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractBoatModel extends EntityModel<BoatRenderState> {
   private final ModelPart leftPaddle;
   private final ModelPart rightPaddle;

   public AbstractBoatModel(ModelPart p_455050_) {
      super(p_455050_);
      this.leftPaddle = p_455050_.getChild("left_paddle");
      this.rightPaddle = p_455050_.getChild("right_paddle");
   }

   public void setupAnim(BoatRenderState p_451522_) {
      super.setupAnim(p_451522_);
      animatePaddle(p_451522_.rowingTimeLeft, 0, this.leftPaddle);
      animatePaddle(p_451522_.rowingTimeRight, 1, this.rightPaddle);
   }

   private static void animatePaddle(float p_455929_, int p_458444_, ModelPart p_450241_) {
      p_450241_.xRot = Mth.clampedLerp((Mth.sin(-p_455929_) + 1.0F) / 2.0F, (float) (-Math.PI / 3), (float) (-Math.PI / 12));
      p_450241_.yRot = Mth.clampedLerp((Mth.sin(-p_455929_ + 1.0F) + 1.0F) / 2.0F, (float) (-Math.PI / 4), (float) (Math.PI / 4));
      if (p_458444_ == 1) {
         p_450241_.yRot = (float) Math.PI - p_450241_.yRot;
      }
   }
}
