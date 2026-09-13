package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractHorseRenderer<T extends AbstractHorse, S extends EquineRenderState, M extends EntityModel<? super S>>
   extends AgeableMobRenderer<T, S, M> {
   public AbstractHorseRenderer(EntityRendererProvider.Context p_173906_, M p_367626_, M p_364695_) {
      super(p_173906_, p_367626_, p_364695_, 0.75F);
   }

   public void extractRenderState(T p_454386_, S p_361124_, float p_366226_) {
      super.extractRenderState(p_454386_, p_361124_, p_366226_);
      p_361124_.saddle = p_454386_.getItemBySlot(EquipmentSlot.SADDLE).copy();
      p_361124_.bodyArmorItem = p_454386_.getBodyArmorItem().copy();
      p_361124_.isRidden = p_454386_.isVehicle();
      p_361124_.eatAnimation = p_454386_.getEatAnim(p_366226_);
      p_361124_.standAnimation = p_454386_.getStandAnim(p_366226_);
      p_361124_.feedingAnimation = p_454386_.getMouthAnim(p_366226_);
      p_361124_.animateTail = p_454386_.tailCounter > 0;
   }
}
