package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractZombieRenderer<T extends Zombie, S extends ZombieRenderState, M extends ZombieModel<S>> extends HumanoidMobRenderer<T, S, M> {
   private static final Identifier ZOMBIE_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");

   protected AbstractZombieRenderer(EntityRendererProvider.Context p_173910_, M p_454811_, M p_460192_, ArmorModelSet<M> p_425173_, ArmorModelSet<M> p_429838_) {
      super(p_173910_, p_454811_, p_460192_, 0.5F);
      this.addLayer(new HumanoidArmorLayer<>(this, p_425173_, p_429838_, p_173910_.getEquipmentRenderer()));
   }

   public Identifier getTextureLocation(S p_451519_) {
      return ZOMBIE_LOCATION;
   }

   public void extractRenderState(T p_452652_, S p_362706_, float p_366302_) {
      super.extractRenderState(p_452652_, p_362706_, p_366302_);
      p_362706_.isAggressive = p_452652_.isAggressive();
      p_362706_.isConverting = p_452652_.isUnderWaterConverting();
   }

   protected boolean isShaking(S p_361791_) {
      return super.isShaking(p_361791_) || p_361791_.isConverting;
   }

   protected HumanoidModel.ArmPose getArmPose(T p_452361_, HumanoidArm p_458869_) {
      SwingAnimation swinganimation = p_452361_.getItemHeldByArm(p_458869_.getOpposite()).get(DataComponents.SWING_ANIMATION);
      return swinganimation != null && swinganimation.type() == SwingAnimationType.STAB ? HumanoidModel.ArmPose.SPEAR : super.getArmPose(p_452361_, p_458869_);
   }
}
