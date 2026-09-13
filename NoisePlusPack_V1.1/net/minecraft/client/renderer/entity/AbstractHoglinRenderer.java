package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.hoglin.HoglinModel;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractHoglinRenderer<T extends Mob & HoglinBase> extends AgeableMobRenderer<T, HoglinRenderState, HoglinModel> {
   public AbstractHoglinRenderer(EntityRendererProvider.Context p_366212_, ModelLayerLocation p_365439_, ModelLayerLocation p_366900_, float p_368524_) {
      super(p_366212_, new HoglinModel(p_366212_.bakeLayer(p_365439_)), new HoglinModel(p_366212_.bakeLayer(p_366900_)), p_368524_);
   }

   public HoglinRenderState createRenderState() {
      return new HoglinRenderState();
   }

   public void extractRenderState(T p_364762_, HoglinRenderState p_364775_, float p_365847_) {
      super.extractRenderState(p_364762_, p_364775_, p_365847_);
      p_364775_.attackAnimationRemainingTicks = p_364762_.getAttackAnimationRemainingTicks();
   }
}
