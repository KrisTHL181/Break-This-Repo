package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElderGuardianParticleGroup extends ParticleGroup<ElderGuardianParticle> {
   public ElderGuardianParticleGroup(ParticleEngine p_426151_) {
      super(p_426151_);
   }

   @Override
   public ParticleGroupRenderState extractRenderState(Frustum p_422468_, Camera p_424768_, float p_431128_) {
      return new ElderGuardianParticleGroup.State(
         this.particles
            .stream()
            .map(p_422579_ -> ElderGuardianParticleGroup.ElderGuardianParticleRenderState.fromParticle(p_422579_, p_424768_, p_431128_))
            .toList()
      );
   }

   @OnlyIn(Dist.CLIENT)
   record ElderGuardianParticleRenderState(Model<Unit> model, PoseStack poseStack, RenderType renderType, int color) {
      public static ElderGuardianParticleGroup.ElderGuardianParticleRenderState fromParticle(ElderGuardianParticle p_430389_, Camera p_426661_, float p_425549_) {
         float f = (p_430389_.age + p_425549_) / p_430389_.lifetime;
         float f1 = 0.05F + 0.5F * Mth.sin(f * (float) Math.PI);
         int i = ARGB.colorFromFloat(f1, 1.0F, 1.0F, 1.0F);
         PoseStack posestack = new PoseStack();
         posestack.pushPose();
         posestack.mulPose(p_426661_.rotation());
         posestack.mulPose(Axis.XP.rotationDegrees(60.0F - 150.0F * f));
         float f2 = 0.42553192F;
         posestack.scale(0.42553192F, -0.42553192F, -0.42553192F);
         posestack.translate(0.0F, -0.56F, 3.5F);
         return new ElderGuardianParticleGroup.ElderGuardianParticleRenderState(p_430389_.model, posestack, p_430389_.renderType, i);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record State(List<ElderGuardianParticleGroup.ElderGuardianParticleRenderState> states) implements ParticleGroupRenderState {
      @Override
      public void submit(SubmitNodeCollector p_429145_, CameraRenderState p_425722_) {
         for (ElderGuardianParticleGroup.ElderGuardianParticleRenderState elderguardianparticlegroup$elderguardianparticlerenderstate : this.states) {
            p_429145_.submitModel(
               elderguardianparticlegroup$elderguardianparticlerenderstate.model,
               Unit.INSTANCE,
               elderguardianparticlegroup$elderguardianparticlerenderstate.poseStack,
               elderguardianparticlegroup$elderguardianparticlerenderstate.renderType,
               15728880,
               OverlayTexture.NO_OVERLAY,
               elderguardianparticlegroup$elderguardianparticlerenderstate.color,
               null,
               0,
               null
            );
         }
      }
   }
}
