package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.hoglin.BabyHoglinModel;
import net.minecraft.client.model.monster.hoglin.HoglinModel;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;

public abstract class AbstractHoglinRenderer<T extends Mob & HoglinBase> extends AgeableMobRenderer<T, HoglinRenderState, HoglinModel> {
   public AbstractHoglinRenderer(
      final EntityRendererProvider.Context context, final ModelLayerLocation adultLayer, final ModelLayerLocation babyLayer, final float shadow
   ) {
      super(context, new HoglinModel(context.bakeLayer(adultLayer)), new BabyHoglinModel(context.bakeLayer(babyLayer)), shadow);
   }

   public HoglinRenderState createRenderState() {
      return new HoglinRenderState();
   }

   public void extractRenderState(final T entity, final HoglinRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.attackAnimationRemainingTicks = entity.getAttackAnimationRemainingTicks();
   }
}
