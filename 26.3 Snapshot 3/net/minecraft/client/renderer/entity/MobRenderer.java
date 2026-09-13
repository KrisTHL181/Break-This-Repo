package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public abstract class MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends LivingEntityRenderer<T, S, M> {
   public MobRenderer(final EntityRendererProvider.Context context, final M model, final float shadow) {
      super(context, model, shadow);
   }

   protected boolean shouldShowName(final T entity, final double distanceToCameraSq) {
      return super.shouldShowName(entity, distanceToCameraSq)
         && (entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
   }

   @Override
   protected float getShadowRadius(final S state) {
      return super.getShadowRadius(state) * state.ageScale;
   }

   protected static boolean checkMagicName(final Entity entity, final String magicName) {
      Component customName = entity.getCustomName();
      return customName != null && magicName.equals(customName.getString());
   }
}
