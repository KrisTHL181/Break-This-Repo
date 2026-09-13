package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FallingLeavesParticle extends SingleQuadParticle {
   private static final float ACCELERATION_SCALE = 0.0025F;
   private static final int INITIAL_LIFETIME = 300;
   private static final int CURVE_ENDPOINT_TIME = 300;
   private float rotSpeed = (float)Math.toRadians(this.random.nextBoolean() ? -30.0 : 30.0);
   private final float spinAcceleration = (float)Math.toRadians(this.random.nextBoolean() ? -5.0 : 5.0);
   private final float windBig;
   private final boolean swirl;
   private final boolean flowAway;
   private final double xaFlowScale;
   private final double zaFlowScale;
   private final double swirlPeriod;

   protected FallingLeavesParticle(
      ClientLevel p_377646_,
      double p_377442_,
      double p_376050_,
      double p_377918_,
      TextureAtlasSprite p_426530_,
      float p_378651_,
      float p_376838_,
      boolean p_378490_,
      boolean p_376930_,
      float p_376718_,
      float p_378174_
   ) {
      super(p_377646_, p_377442_, p_376050_, p_377918_, p_426530_);
      this.windBig = p_376838_;
      this.swirl = p_378490_;
      this.flowAway = p_376930_;
      this.lifetime = 300;
      this.gravity = p_378651_ * 1.2F * 0.0025F;
      float f = p_376718_ * (this.random.nextBoolean() ? 0.05F : 0.075F);
      this.quadSize = f;
      this.setSize(f, f);
      this.friction = 1.0F;
      this.yd = -p_378174_;
      float f1 = this.random.nextFloat();
      this.xaFlowScale = Math.cos(Math.toRadians(f1 * 60.0F)) * this.windBig;
      this.zaFlowScale = Math.sin(Math.toRadians(f1 * 60.0F)) * this.windBig;
      this.swirlPeriod = Math.toRadians(1000.0F + f1 * 3000.0F);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.lifetime-- <= 0) {
         this.remove();
      }

      if (!this.removed) {
         float f = 300 - this.lifetime;
         float f1 = Math.min(f / 300.0F, 1.0F);
         double d0 = 0.0;
         double d1 = 0.0;
         if (this.flowAway) {
            d0 += this.xaFlowScale * Math.pow(f1, 1.25);
            d1 += this.zaFlowScale * Math.pow(f1, 1.25);
         }

         if (this.swirl) {
            d0 += f1 * Math.cos(f1 * this.swirlPeriod) * this.windBig;
            d1 += f1 * Math.sin(f1 * this.swirlPeriod) * this.windBig;
         }

         this.xd += d0 * 0.0025F;
         this.zd += d1 * 0.0025F;
         this.yd = this.yd - this.gravity;
         this.rotSpeed = this.rotSpeed + this.spinAcceleration / 20.0F;
         this.oRoll = this.roll;
         this.roll = this.roll + this.rotSpeed / 20.0F;
         this.move(this.xd, this.yd, this.zd);
         if (this.onGround || this.lifetime < 299 && (this.xd == 0.0 || this.zd == 0.0)) {
            this.remove();
         }

         if (!this.removed) {
            this.xd = this.xd * this.friction;
            this.yd = this.yd * this.friction;
            this.zd = this.zd * this.friction;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class CherryProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public CherryProvider(SpriteSet p_376778_) {
         this.sprites = p_376778_;
      }

      public Particle createParticle(
         SimpleParticleType p_429629_,
         ClientLevel p_375913_,
         double p_375714_,
         double p_376515_,
         double p_376801_,
         double p_378662_,
         double p_376463_,
         double p_377178_,
         RandomSource p_428467_
      ) {
         return new FallingLeavesParticle(p_375913_, p_375714_, p_376515_, p_376801_, this.sprites.get(p_428467_), 0.25F, 2.0F, false, true, 1.0F, 0.0F);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class PaleOakProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public PaleOakProvider(SpriteSet p_378488_) {
         this.sprites = p_378488_;
      }

      public Particle createParticle(
         SimpleParticleType p_423653_,
         ClientLevel p_377367_,
         double p_378534_,
         double p_375460_,
         double p_376536_,
         double p_377840_,
         double p_375925_,
         double p_378165_,
         RandomSource p_427922_
      ) {
         return new FallingLeavesParticle(p_377367_, p_378534_, p_375460_, p_376536_, this.sprites.get(p_427922_), 0.07F, 10.0F, true, false, 2.0F, 0.021F);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class TintedLeavesProvider implements ParticleProvider<ColorParticleOption> {
      private final SpriteSet sprites;

      public TintedLeavesProvider(SpriteSet p_394361_) {
         this.sprites = p_394361_;
      }

      public Particle createParticle(
         ColorParticleOption p_391473_,
         ClientLevel p_391175_,
         double p_394602_,
         double p_394318_,
         double p_392484_,
         double p_391926_,
         double p_393741_,
         double p_395481_,
         RandomSource p_429124_
      ) {
         FallingLeavesParticle fallingleavesparticle = new FallingLeavesParticle(
            p_391175_, p_394602_, p_394318_, p_392484_, this.sprites.get(p_429124_), 0.07F, 10.0F, true, false, 2.0F, 0.021F
         );
         fallingleavesparticle.setColor(p_391473_.getRed(), p_391473_.getGreen(), p_391473_.getBlue());
         return fallingleavesparticle;
      }
   }
}
