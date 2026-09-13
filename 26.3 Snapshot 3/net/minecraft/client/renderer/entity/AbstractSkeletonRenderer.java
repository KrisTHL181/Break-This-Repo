package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.Items;

public abstract class AbstractSkeletonRenderer<T extends AbstractSkeleton, S extends SkeletonRenderState> extends HumanoidMobRenderer<T, S, SkeletonModel<S>> {
   public AbstractSkeletonRenderer(
      final EntityRendererProvider.Context context, final ModelLayerLocation body, final ArmorModelSet<ModelLayerLocation> armorSet
   ) {
      this(context, armorSet, new SkeletonModel<>(context.bakeLayer(body)));
   }

   public AbstractSkeletonRenderer(
      final EntityRendererProvider.Context context, final ArmorModelSet<ModelLayerLocation> armorSet, final SkeletonModel<S> bodyModel
   ) {
      super(context, bodyModel, 0.5F);
      this.addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(armorSet, context.getModelSet(), SkeletonModel::new), context.getEquipmentRenderer()));
   }

   public void extractRenderState(final T entity, final S state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isAggressive = entity.isAggressive();
      state.isShaking = entity.isShaking();
      state.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
   }

   protected boolean isShaking(final S state) {
      return state.isShaking;
   }

   protected HumanoidModel.ArmPose getArmPose(final T mob, final HumanoidArm arm) {
      return mob.getMainArm() == arm && mob.isAggressive() && mob.getMainHandItem().is(Items.BOW)
         ? HumanoidModel.ArmPose.BOW_AND_ARROW
         : super.getArmPose(mob, arm);
   }
}
