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

public class ElderGuardianParticle extends Particle {
   protected final GuardianParticleModel model;
   protected final RenderType renderType = RenderTypes.entityTranslucent(ElderGuardianRenderer.GUARDIAN_ELDER_LOCATION);

   private ElderGuardianParticle(final ClientLevel level, final double x, final double y, final double z) {
      super(level, x, y, z);
      this.model = new GuardianParticleModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELDER_GUARDIAN));
      this.gravity = 0.0F;
      this.lifetime = 30;
   }

   @Override
   public ParticleRenderType getGroup() {
      return ParticleRenderType.ELDER_GUARDIANS;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      public Particle createParticle(
         final SimpleParticleType options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         return new ElderGuardianParticle(level, x, y, z);
      }
   }
}
