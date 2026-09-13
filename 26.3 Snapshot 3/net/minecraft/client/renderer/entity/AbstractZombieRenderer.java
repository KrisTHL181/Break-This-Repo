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

public abstract class AbstractZombieRenderer<T extends Zombie, S extends ZombieRenderState, M extends ZombieModel<S>> extends HumanoidMobRenderer<T, S, M> {
   private static final Identifier ZOMBIE_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");
   private static final Identifier BABY_ZOMBIE_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/zombie_baby.png");

   protected AbstractZombieRenderer(
      final EntityRendererProvider.Context context, final M model, final M babyModel, final ArmorModelSet<M> armorSet, final ArmorModelSet<M> babyArmorSet
   ) {
      super(context, model, babyModel, 0.5F);
      this.addLayer(new HumanoidArmorLayer<>(this, armorSet, babyArmorSet, context.getEquipmentRenderer()));
   }

   public Identifier getTextureLocation(final S state) {
      return state.isBaby ? BABY_ZOMBIE_LOCATION : ZOMBIE_LOCATION;
   }

   public void extractRenderState(final T entity, final S state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isAggressive = entity.isAggressive();
      state.isConverting = entity.isUnderWaterConverting();
   }

   protected boolean isShaking(final S state) {
      return super.isShaking(state) || state.isConverting;
   }

   protected HumanoidModel.ArmPose getArmPose(final T mob, final HumanoidArm arm) {
      SwingAnimation otherAnim = mob.getItemHeldByArm(arm.getOpposite()).get(DataComponents.SWING_ANIMATION);
      return otherAnim != null && otherAnim.type() == SwingAnimationType.STAB ? HumanoidModel.ArmPose.SPEAR : super.getArmPose(mob, arm);
   }
}
