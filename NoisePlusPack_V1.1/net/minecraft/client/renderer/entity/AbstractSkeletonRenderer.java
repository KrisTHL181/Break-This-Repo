package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSkeletonRenderer<T extends AbstractSkeleton, S extends SkeletonRenderState> extends HumanoidMobRenderer<T, S, SkeletonModel<S>> {
   public AbstractSkeletonRenderer(EntityRendererProvider.Context p_362142_, ModelLayerLocation p_369786_, ArmorModelSet<ModelLayerLocation> p_424115_) {
      this(p_362142_, p_424115_, new SkeletonModel<>(p_362142_.bakeLayer(p_369786_)));
   }

   public AbstractSkeletonRenderer(EntityRendererProvider.Context p_363000_, ArmorModelSet<ModelLayerLocation> p_425914_, SkeletonModel<S> p_453201_) {
      super(p_363000_, p_453201_, 0.5F);
      this.addLayer(
         new HumanoidArmorLayer<>(this, ArmorModelSet.bake(p_425914_, p_363000_.getModelSet(), SkeletonModel::new), p_363000_.getEquipmentRenderer())
      );
   }

   public void extractRenderState(T p_453418_, S p_363603_, float p_362928_) {
      super.extractRenderState(p_453418_, p_363603_, p_362928_);
      p_363603_.isAggressive = p_453418_.isAggressive();
      p_363603_.isShaking = p_453418_.isShaking();
      p_363603_.isHoldingBow = p_453418_.getMainHandItem().is(Items.BOW);
   }

   protected boolean isShaking(S p_366804_) {
      return p_366804_.isShaking;
   }

   protected HumanoidModel.ArmPose getArmPose(T p_457780_, HumanoidArm p_376655_) {
      return p_457780_.getMainArm() == p_376655_ && p_457780_.isAggressive() && p_457780_.getMainHandItem().is(Items.BOW)
         ? HumanoidModel.ArmPose.BOW_AND_ARROW
         : super.getArmPose(p_457780_, p_376655_);
   }
}
