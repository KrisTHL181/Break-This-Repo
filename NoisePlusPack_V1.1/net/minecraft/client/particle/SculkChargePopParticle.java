package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SculkChargePopParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   SculkChargePopParticle(
      ClientLevel p_233932_, double p_233933_, double p_233934_, double p_233935_, double p_233936_, double p_233937_, double p_233938_, SpriteSet p_233939_
   ) {
      super(p_233932_, p_233933_, p_233934_, p_233935_, p_233936_, p_233937_, p_233938_, p_233939_.first());
      this.friction = 0.96F;
      this.sprites = p_233939_;
      this.scale(1.0F);
      this.hasPhysics = false;
      this.setSpriteFromAge(p_233939_);
   }

   @Override
   public int getLightColor(float p_233942_) {
      return 240;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   @OnlyIn(Dist.CLIENT)
   public record Provider(SpriteSet sprite) implements ParticleProvider<SimpleParticleType> {
      public Particle createParticle(
         SimpleParticleType p_233958_,
         ClientLevel p_233959_,
         double p_233960_,
         double p_233961_,
         double p_233962_,
         double p_233963_,
         double p_233964_,
         double p_233965_,
         RandomSource p_424852_
      ) {
         SculkChargePopParticle sculkchargepopparticle = new SculkChargePopParticle(
            p_233959_, p_233960_, p_233961_, p_233962_, p_233963_, p_233964_, p_233965_, this.sprite
         );
         sculkchargepopparticle.setAlpha(1.0F);
         sculkchargepopparticle.setParticleSpeed(p_233963_, p_233964_, p_233965_);
         sculkchargepopparticle.setLifetime(p_424852_.nextInt(4) + 6);
         return sculkchargepopparticle;
      }
   }
}
