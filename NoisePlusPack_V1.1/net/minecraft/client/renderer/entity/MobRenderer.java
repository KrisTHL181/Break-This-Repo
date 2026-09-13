package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends LivingEntityRenderer<T, S, M> {
   public MobRenderer(EntityRendererProvider.Context p_174304_, M p_174305_, float p_174306_) {
      super(p_174304_, p_174305_, p_174306_);
   }

   protected boolean shouldShowName(T p_360956_, double p_369719_) {
      return super.shouldShowName(p_360956_, p_369719_)
         && (p_360956_.shouldShowName() || p_360956_.hasCustomName() && p_360956_ == this.entityRenderDispatcher.crosshairPickEntity);
   }

   @Override
   protected float getShadowRadius(S p_365071_) {
      return super.getShadowRadius(p_365071_) * p_365071_.ageScale;
   }

   protected static boolean checkMagicName(Entity p_428462_, String p_430656_) {
      Component component = p_428462_.getCustomName();
      return component != null && p_430656_.equals(component.getString());
   }
}
