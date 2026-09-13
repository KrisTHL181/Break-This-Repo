package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CritParticle extends SingleQuadParticle {
   CritParticle(
      ClientLevel p_105919_,
      double p_105920_,
      double p_105921_,
      double p_105922_,
      double p_105923_,
      double p_105924_,
      double p_105925_,
      TextureAtlasSprite p_428872_
   ) {
      super(p_105919_, p_105920_, p_105921_, p_105922_, 0.0, 0.0, 0.0, p_428872_);
      this.friction = 0.7F;
      this.gravity = 0.5F;
      this.xd *= 0.1F;
      this.yd *= 0.1F;
      this.zd *= 0.1F;
      this.xd += p_105923_ * 0.4;
      this.yd += p_105924_ * 0.4;
      this.zd += p_105925_ * 0.4;
      float f = this.random.nextFloat() * 0.3F + 0.6F;
      this.rCol = f;
      this.gCol = f;
      this.bCol = f;
      this.quadSize *= 0.75F;
      this.lifetime = Math.max((int)(6.0 / (this.random.nextFloat() * 0.8 + 0.6)), 1);
      this.hasPhysics = false;
      this.tick();
   }

   @Override
   public float getQuadSize(float p_105938_) {
      return this.quadSize * Mth.clamp((this.age + p_105938_) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      this.gCol *= 0.96F;
      this.bCol *= 0.9F;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @OnlyIn(Dist.CLIENT)
   public static class DamageIndicatorProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DamageIndicatorProvider(SpriteSet p_105941_) {
         this.sprite = p_105941_;
      }

      public Particle createParticle(
         SimpleParticleType p_427638_,
         ClientLevel p_105944_,
         double p_105945_,
         double p_105946_,
         double p_105947_,
         double p_105948_,
         double p_105949_,
         double p_105950_,
         RandomSource p_427675_
      ) {
         CritParticle critparticle = new CritParticle(
            p_105944_, p_105945_, p_105946_, p_105947_, p_105948_, p_105949_ + 1.0, p_105950_, this.sprite.get(p_427675_)
         );
         critparticle.setLifetime(20);
         return critparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class MagicProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public MagicProvider(SpriteSet p_105962_) {
         this.sprite = p_105962_;
      }

      public Particle createParticle(
         SimpleParticleType p_424905_,
         ClientLevel p_105965_,
         double p_105966_,
         double p_105967_,
         double p_105968_,
         double p_105969_,
         double p_105970_,
         double p_105971_,
         RandomSource p_425027_
      ) {
         CritParticle critparticle = new CritParticle(p_105965_, p_105966_, p_105967_, p_105968_, p_105969_, p_105970_, p_105971_, this.sprite.get(p_425027_));
         critparticle.rCol *= 0.3F;
         critparticle.gCol *= 0.8F;
         return critparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_105983_) {
         this.sprite = p_105983_;
      }

      public Particle createParticle(
         SimpleParticleType p_423798_,
         ClientLevel p_105986_,
         double p_105987_,
         double p_105988_,
         double p_105989_,
         double p_105990_,
         double p_105991_,
         double p_105992_,
         RandomSource p_423877_
      ) {
         return new CritParticle(p_105986_, p_105987_, p_105988_, p_105989_, p_105990_, p_105991_, p_105992_, this.sprite.get(p_423877_));
      }
   }
}
