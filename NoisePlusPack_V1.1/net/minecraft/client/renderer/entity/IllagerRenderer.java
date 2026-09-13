package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.item.CrossbowItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class IllagerRenderer<T extends AbstractIllager, S extends IllagerRenderState> extends MobRenderer<T, S, IllagerModel<S>> {
   protected IllagerRenderer(EntityRendererProvider.Context p_174182_, IllagerModel<S> p_456967_, float p_174184_) {
      super(p_174182_, p_456967_, p_174184_);
      this.addLayer(new CustomHeadLayer<>(this, p_174182_.getModelSet(), p_174182_.getPlayerSkinRenderCache()));
   }

   public void extractRenderState(T p_457871_, S p_365392_, float p_369885_) {
      super.extractRenderState(p_457871_, p_365392_, p_369885_);
      ArmedEntityRenderState.extractArmedEntityRenderState(p_457871_, p_365392_, this.itemModelResolver, p_369885_);
      p_365392_.isRiding = p_457871_.isPassenger();
      p_365392_.mainArm = p_457871_.getMainArm();
      p_365392_.armPose = p_457871_.getArmPose();
      p_365392_.maxCrossbowChargeDuration = p_365392_.armPose == AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE
         ? CrossbowItem.getChargeDuration(p_457871_.getUseItem(), p_457871_)
         : 0;
      p_365392_.ticksUsingItem = p_457871_.getTicksUsingItem(p_369885_);
      p_365392_.attackAnim = p_457871_.getAttackAnim(p_369885_);
      p_365392_.isAggressive = p_457871_.isAggressive();
   }
}
