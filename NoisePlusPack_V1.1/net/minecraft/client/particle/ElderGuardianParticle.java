package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.guardian.GuardianParticleModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.ElderGuardianRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElderGuardianParticle extends Particle {
   protected final GuardianParticleModel model;
   protected final RenderType renderType = RenderTypes.entityTranslucent(ElderGuardianRenderer.GUARDIAN_ELDER_LOCATION);

   ElderGuardianParticle(ClientLevel p_422689_, double p_426144_, double p_429075_, double p_428402_) {
      super(p_422689_, p_426144_, p_429075_, p_428402_);
      this.model = new GuardianParticleModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELDER_GUARDIAN));
      this.gravity = 0.0F;
      this.lifetime = 30;
   }

   @Override
   public ParticleRenderType getGroup() {
      return ParticleRenderType.ELDER_GUARDIANS;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      public Particle createParticle(
         SimpleParticleType p_426396_,
         ClientLevel p_427577_,
         double p_425185_,
         double p_427390_,
         double p_423786_,
         double p_430470_,
         double p_431048_,
         double p_423926_,
         RandomSource p_429308_
      ) {
         return new ElderGuardianParticle(p_427577_, p_425185_, p_427390_, p_423786_);
      }
   }
}
