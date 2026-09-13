package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Mob;

@Deprecated
public abstract class AgeableMobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends MobRenderer<T, S, M> {
   private final M adultModel;
   private final M babyModel;

   public AgeableMobRenderer(final EntityRendererProvider.Context context, final M adultModel, final M babyModel, final float shadow) {
      super(context, adultModel, shadow);
      this.adultModel = adultModel;
      this.babyModel = babyModel;
   }

   @Override
   public void submit(final S state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      this.model = state.isBaby ? this.babyModel : this.adultModel;
      super.submit(state, poseStack, submitNodeCollector, camera);
   }
}
