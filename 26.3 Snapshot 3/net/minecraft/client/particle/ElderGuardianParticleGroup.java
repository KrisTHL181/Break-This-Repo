package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;

public class ElderGuardianParticleGroup extends ParticleGroup<ElderGuardianParticle> {
   public ElderGuardianParticleGroup(final ParticleEngine engine) {
      super(engine);
   }

   @Override
   public ParticleGroupRenderState extractRenderState(final Frustum frustum, final Camera camera, final float partialTickTime) {
      return new ElderGuardianParticleGroup.State(
         this.particles
            .stream()
            .map(particle -> ElderGuardianParticleGroup.ElderGuardianParticleRenderState.fromParticle(particle, camera, partialTickTime))
            .toList()
      );
   }

   private record ElderGuardianParticleRenderState(Model<Unit> model, PoseStack poseStack, RenderType renderType, int color) {
      public static ElderGuardianParticleGroup.ElderGuardianParticleRenderState fromParticle(
         final ElderGuardianParticle particle, final Camera camera, final float partialTickTime
      ) {
         float ageScale = (particle.age + partialTickTime) / particle.lifetime;
         float alpha = 0.05F + 0.5F * Mth.sin(ageScale * (float) Math.PI);
         int color = ARGB.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);
         PoseStack poseStack = new PoseStack();
         poseStack.pushPose();
         poseStack.mulPose(camera.rotation());
         poseStack.mulPose(Axis.XP.rotationDegrees(60.0F - 150.0F * ageScale));
         float scale = 0.42553192F;
         poseStack.scale(0.42553192F, -0.42553192F, -0.42553192F);
         poseStack.translate(0.0F, -0.56F, 3.5F);
         return new ElderGuardianParticleGroup.ElderGuardianParticleRenderState(particle.model, poseStack, particle.renderType, color);
      }
   }

   private record State(List<ElderGuardianParticleGroup.ElderGuardianParticleRenderState> states) implements ParticleGroupRenderState {
      @Override
      public void submit(final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
         for (ElderGuardianParticleGroup.ElderGuardianParticleRenderState state : this.states) {
            submitNodeCollector.submitModel(
               state.model, Unit.INSTANCE, state.poseStack, state.renderType, 15728880, OverlayTexture.NO_OVERLAY, state.color, null, 0
            );
         }
      }
   }
}
