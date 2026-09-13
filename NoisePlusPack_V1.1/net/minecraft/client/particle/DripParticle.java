package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DripParticle extends SingleQuadParticle {
   private final Fluid type;
   protected boolean isGlowing;

   DripParticle(ClientLevel p_106051_, double p_106052_, double p_106053_, double p_106054_, Fluid p_106055_, TextureAtlasSprite p_425703_) {
      super(p_106051_, p_106052_, p_106053_, p_106054_, p_425703_);
      this.setSize(0.01F, 0.01F);
      this.gravity = 0.06F;
      this.type = p_106055_;
   }

   protected Fluid getType() {
      return this.type;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public int getLightColor(float p_106065_) {
      return this.isGlowing ? 240 : super.getLightColor(p_106065_);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.preMoveUpdate();
      if (!this.removed) {
         this.yd = this.yd - this.gravity;
         this.move(this.xd, this.yd, this.zd);
         this.postMoveUpdate();
         if (!this.removed) {
            this.xd *= 0.98F;
            this.yd *= 0.98F;
            this.zd *= 0.98F;
            if (this.type != Fluids.EMPTY) {
               BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
               FluidState fluidstate = this.level.getFluidState(blockpos);
               if (fluidstate.getType() == this.type && this.y < blockpos.getY() + fluidstate.getHeight(this.level, blockpos)) {
                  this.remove();
               }
            }
         }
      }
   }

   protected void preMoveUpdate() {
      if (this.lifetime-- <= 0) {
         this.remove();
      }
   }

   protected void postMoveUpdate() {
   }

   @OnlyIn(Dist.CLIENT)
   static class CoolingDripHangParticle extends DripParticle.DripHangParticle {
      CoolingDripHangParticle(
         ClientLevel p_106068_, double p_106069_, double p_106070_, double p_106071_, Fluid p_106072_, ParticleOptions p_106073_, TextureAtlasSprite p_422484_
      ) {
         super(p_106068_, p_106069_, p_106070_, p_106071_, p_106072_, p_106073_, p_422484_);
      }

      @Override
      protected void preMoveUpdate() {
         this.rCol = 1.0F;
         this.gCol = 16.0F / (40 - this.lifetime + 16);
         this.bCol = 4.0F / (40 - this.lifetime + 8);
         super.preMoveUpdate();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DripHangParticle extends DripParticle {
      private final ParticleOptions fallingParticle;

      DripHangParticle(
         ClientLevel p_106085_, double p_106086_, double p_106087_, double p_106088_, Fluid p_106089_, ParticleOptions p_106090_, TextureAtlasSprite p_426523_
      ) {
         super(p_106085_, p_106086_, p_106087_, p_106088_, p_106089_, p_426523_);
         this.fallingParticle = p_106090_;
         this.gravity *= 0.02F;
         this.lifetime = 40;
      }

      @Override
      protected void preMoveUpdate() {
         if (this.lifetime-- <= 0) {
            this.remove();
            this.level.addParticle(this.fallingParticle, this.x, this.y, this.z, this.xd, this.yd, this.zd);
         }
      }

      @Override
      protected void postMoveUpdate() {
         this.xd *= 0.02;
         this.yd *= 0.02;
         this.zd *= 0.02;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DripLandParticle extends DripParticle {
      DripLandParticle(ClientLevel p_106102_, double p_106103_, double p_106104_, double p_106105_, Fluid p_106106_, TextureAtlasSprite p_428791_) {
         super(p_106102_, p_106103_, p_106104_, p_106105_, p_106106_, p_428791_);
         this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2));
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DripstoneFallAndLandParticle extends DripParticle.FallAndLandParticle {
      DripstoneFallAndLandParticle(
         ClientLevel p_171930_, double p_171931_, double p_171932_, double p_171933_, Fluid p_171934_, ParticleOptions p_171935_, TextureAtlasSprite p_426375_
      ) {
         super(p_171930_, p_171931_, p_171932_, p_171933_, p_171934_, p_171935_, p_426375_);
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            SoundEvent soundevent = this.getType() == Fluids.LAVA ? SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
            float f = Mth.randomBetween(this.random, 0.3F, 1.0F);
            this.level.playLocalSound(this.x, this.y, this.z, soundevent, SoundSource.BLOCKS, f, 1.0F, false);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DripstoneLavaFallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DripstoneLavaFallProvider(SpriteSet p_425758_) {
         this.sprite = p_425758_;
      }

      public Particle createParticle(
         SimpleParticleType p_431761_,
         ClientLevel p_423111_,
         double p_423193_,
         double p_429045_,
         double p_425867_,
         double p_428760_,
         double p_430409_,
         double p_431177_,
         RandomSource p_429203_
      ) {
         DripParticle dripparticle = new DripParticle.DripstoneFallAndLandParticle(
            p_423111_, p_423193_, p_429045_, p_425867_, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.sprite.get(p_429203_)
         );
         dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DripstoneLavaHangProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DripstoneLavaHangProvider(SpriteSet p_423905_) {
         this.sprite = p_423905_;
      }

      public Particle createParticle(
         SimpleParticleType p_424794_,
         ClientLevel p_423459_,
         double p_428158_,
         double p_422283_,
         double p_425126_,
         double p_430288_,
         double p_423321_,
         double p_425833_,
         RandomSource p_426968_
      ) {
         return new DripParticle.CoolingDripHangParticle(
            p_423459_, p_428158_, p_422283_, p_425126_, Fluids.LAVA, ParticleTypes.FALLING_DRIPSTONE_LAVA, this.sprite.get(p_426968_)
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DripstoneWaterFallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DripstoneWaterFallProvider(SpriteSet p_425645_) {
         this.sprite = p_425645_;
      }

      public Particle createParticle(
         SimpleParticleType p_431734_,
         ClientLevel p_426086_,
         double p_431341_,
         double p_426470_,
         double p_427258_,
         double p_425165_,
         double p_426117_,
         double p_425827_,
         RandomSource p_422314_
      ) {
         DripParticle dripparticle = new DripParticle.DripstoneFallAndLandParticle(
            p_426086_, p_431341_, p_426470_, p_427258_, Fluids.WATER, ParticleTypes.SPLASH, this.sprite.get(p_422314_)
         );
         dripparticle.setColor(0.2F, 0.3F, 1.0F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DripstoneWaterHangProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DripstoneWaterHangProvider(SpriteSet p_426371_) {
         this.sprite = p_426371_;
      }

      public Particle createParticle(
         SimpleParticleType p_426350_,
         ClientLevel p_426952_,
         double p_424342_,
         double p_425098_,
         double p_427047_,
         double p_422481_,
         double p_422336_,
         double p_429791_,
         RandomSource p_423434_
      ) {
         DripParticle dripparticle = new DripParticle.DripHangParticle(
            p_426952_, p_424342_, p_425098_, p_427047_, Fluids.WATER, ParticleTypes.FALLING_DRIPSTONE_WATER, this.sprite.get(p_423434_)
         );
         dripparticle.setColor(0.2F, 0.3F, 1.0F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class FallAndLandParticle extends DripParticle.FallingParticle {
      protected final ParticleOptions landParticle;

      FallAndLandParticle(
         ClientLevel p_106116_, double p_106117_, double p_106118_, double p_106119_, Fluid p_106120_, ParticleOptions p_106121_, TextureAtlasSprite p_428169_
      ) {
         super(p_106116_, p_106117_, p_106118_, p_106119_, p_106120_, p_428169_);
         this.lifetime = (int)(64.0 / (this.random.nextFloat() * 0.8 + 0.2));
         this.landParticle = p_106121_;
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class FallingParticle extends DripParticle {
      FallingParticle(ClientLevel p_106132_, double p_106133_, double p_106134_, double p_106135_, Fluid p_106136_, TextureAtlasSprite p_431130_) {
         super(p_106132_, p_106133_, p_106134_, p_106135_, p_106136_, p_431130_);
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class HoneyFallAndLandParticle extends DripParticle.FallAndLandParticle {
      HoneyFallAndLandParticle(
         ClientLevel p_106146_, double p_106147_, double p_106148_, double p_106149_, Fluid p_106150_, ParticleOptions p_106151_, TextureAtlasSprite p_431727_
      ) {
         super(p_106146_, p_106147_, p_106148_, p_106149_, p_106150_, p_106151_, p_431727_);
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            float f = Mth.randomBetween(this.random, 0.3F, 1.0F);
            this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, f, 1.0F, false);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class HoneyFallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public HoneyFallProvider(SpriteSet p_431510_) {
         this.sprite = p_431510_;
      }

      public Particle createParticle(
         SimpleParticleType p_430146_,
         ClientLevel p_424420_,
         double p_423432_,
         double p_426096_,
         double p_427184_,
         double p_425756_,
         double p_424220_,
         double p_427989_,
         RandomSource p_423144_
      ) {
         DripParticle dripparticle = new DripParticle.HoneyFallAndLandParticle(
            p_424420_, p_423432_, p_426096_, p_427184_, Fluids.EMPTY, ParticleTypes.LANDING_HONEY, this.sprite.get(p_423144_)
         );
         dripparticle.gravity = 0.01F;
         dripparticle.setColor(0.582F, 0.448F, 0.082F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class HoneyHangProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public HoneyHangProvider(SpriteSet p_429871_) {
         this.sprite = p_429871_;
      }

      public Particle createParticle(
         SimpleParticleType p_431342_,
         ClientLevel p_426133_,
         double p_430012_,
         double p_423742_,
         double p_425282_,
         double p_431550_,
         double p_429144_,
         double p_423952_,
         RandomSource p_425667_
      ) {
         DripParticle.DripHangParticle dripparticle$driphangparticle = new DripParticle.DripHangParticle(
            p_426133_, p_430012_, p_423742_, p_425282_, Fluids.EMPTY, ParticleTypes.FALLING_HONEY, this.sprite.get(p_425667_)
         );
         dripparticle$driphangparticle.gravity *= 0.01F;
         dripparticle$driphangparticle.lifetime = 100;
         dripparticle$driphangparticle.setColor(0.622F, 0.508F, 0.082F);
         return dripparticle$driphangparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class HoneyLandProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public HoneyLandProvider(SpriteSet p_430948_) {
         this.sprite = p_430948_;
      }

      public Particle createParticle(
         SimpleParticleType p_424073_,
         ClientLevel p_426607_,
         double p_431500_,
         double p_428954_,
         double p_427278_,
         double p_428621_,
         double p_428764_,
         double p_427861_,
         RandomSource p_425089_
      ) {
         DripParticle dripparticle = new DripParticle.DripLandParticle(p_426607_, p_431500_, p_428954_, p_427278_, Fluids.EMPTY, this.sprite.get(p_425089_));
         dripparticle.lifetime = (int)(128.0 / (p_425089_.nextFloat() * 0.8 + 0.2));
         dripparticle.setColor(0.522F, 0.408F, 0.082F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LavaFallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public LavaFallProvider(SpriteSet p_426814_) {
         this.sprite = p_426814_;
      }

      public Particle createParticle(
         SimpleParticleType p_430452_,
         ClientLevel p_430073_,
         double p_425803_,
         double p_425775_,
         double p_424094_,
         double p_423889_,
         double p_423113_,
         double p_429886_,
         RandomSource p_429853_
      ) {
         DripParticle dripparticle = new DripParticle.FallAndLandParticle(
            p_430073_, p_425803_, p_425775_, p_424094_, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.sprite.get(p_429853_)
         );
         dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LavaHangProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public LavaHangProvider(SpriteSet p_426853_) {
         this.sprite = p_426853_;
      }

      public Particle createParticle(
         SimpleParticleType p_429493_,
         ClientLevel p_425119_,
         double p_424967_,
         double p_423365_,
         double p_423925_,
         double p_428453_,
         double p_427441_,
         double p_426773_,
         RandomSource p_424574_
      ) {
         return new DripParticle.CoolingDripHangParticle(
            p_425119_, p_424967_, p_423365_, p_423925_, Fluids.LAVA, ParticleTypes.FALLING_LAVA, this.sprite.get(p_424574_)
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LavaLandProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public LavaLandProvider(SpriteSet p_423185_) {
         this.sprite = p_423185_;
      }

      public Particle createParticle(
         SimpleParticleType p_423512_,
         ClientLevel p_430817_,
         double p_429211_,
         double p_427244_,
         double p_425070_,
         double p_427679_,
         double p_426578_,
         double p_422948_,
         RandomSource p_429567_
      ) {
         DripParticle dripparticle = new DripParticle.DripLandParticle(p_430817_, p_429211_, p_427244_, p_425070_, Fluids.LAVA, this.sprite.get(p_429567_));
         dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class NectarFallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public NectarFallProvider(SpriteSet p_423052_) {
         this.sprite = p_423052_;
      }

      public Particle createParticle(
         SimpleParticleType p_423984_,
         ClientLevel p_426225_,
         double p_425277_,
         double p_424080_,
         double p_429348_,
         double p_427525_,
         double p_428027_,
         double p_423597_,
         RandomSource p_427593_
      ) {
         DripParticle dripparticle = new DripParticle.FallingParticle(p_426225_, p_425277_, p_424080_, p_429348_, Fluids.EMPTY, this.sprite.get(p_427593_));
         dripparticle.lifetime = (int)(16.0 / (p_427593_.nextFloat() * 0.8 + 0.2));
         dripparticle.gravity = 0.007F;
         dripparticle.setColor(0.92F, 0.782F, 0.72F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ObsidianTearFallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ObsidianTearFallProvider(SpriteSet p_424940_) {
         this.sprite = p_424940_;
      }

      public Particle createParticle(
         SimpleParticleType p_426091_,
         ClientLevel p_424298_,
         double p_425109_,
         double p_424455_,
         double p_423151_,
         double p_427300_,
         double p_423406_,
         double p_425906_,
         RandomSource p_427540_
      ) {
         DripParticle dripparticle = new DripParticle.FallAndLandParticle(
            p_424298_, p_425109_, p_424455_, p_423151_, Fluids.EMPTY, ParticleTypes.LANDING_OBSIDIAN_TEAR, this.sprite.get(p_427540_)
         );
         dripparticle.isGlowing = true;
         dripparticle.gravity = 0.01F;
         dripparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ObsidianTearHangProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ObsidianTearHangProvider(SpriteSet p_422557_) {
         this.sprite = p_422557_;
      }

      public Particle createParticle(
         SimpleParticleType p_428793_,
         ClientLevel p_426759_,
         double p_427982_,
         double p_427820_,
         double p_423929_,
         double p_428317_,
         double p_424973_,
         double p_431532_,
         RandomSource p_427422_
      ) {
         DripParticle.DripHangParticle dripparticle$driphangparticle = new DripParticle.DripHangParticle(
            p_426759_, p_427982_, p_427820_, p_423929_, Fluids.EMPTY, ParticleTypes.FALLING_OBSIDIAN_TEAR, this.sprite.get(p_427422_)
         );
         dripparticle$driphangparticle.isGlowing = true;
         dripparticle$driphangparticle.gravity *= 0.01F;
         dripparticle$driphangparticle.lifetime = 100;
         dripparticle$driphangparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
         return dripparticle$driphangparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ObsidianTearLandProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ObsidianTearLandProvider(SpriteSet p_424072_) {
         this.sprite = p_424072_;
      }

      public Particle createParticle(
         SimpleParticleType p_429699_,
         ClientLevel p_422808_,
         double p_422834_,
         double p_425654_,
         double p_429899_,
         double p_424956_,
         double p_431190_,
         double p_427799_,
         RandomSource p_431354_
      ) {
         DripParticle dripparticle = new DripParticle.DripLandParticle(p_422808_, p_422834_, p_425654_, p_429899_, Fluids.EMPTY, this.sprite.get(p_431354_));
         dripparticle.isGlowing = true;
         dripparticle.lifetime = (int)(28.0 / (p_431354_.nextFloat() * 0.8 + 0.2));
         dripparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SporeBlossomFallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SporeBlossomFallProvider(SpriteSet p_424817_) {
         this.sprite = p_424817_;
      }

      public Particle createParticle(
         SimpleParticleType p_423062_,
         ClientLevel p_425919_,
         double p_422663_,
         double p_430981_,
         double p_427973_,
         double p_428306_,
         double p_428840_,
         double p_430740_,
         RandomSource p_431187_
      ) {
         DripParticle dripparticle = new DripParticle.FallingParticle(p_425919_, p_422663_, p_430981_, p_427973_, Fluids.EMPTY, this.sprite.get(p_431187_));
         dripparticle.lifetime = (int)(64.0F / Mth.randomBetween(dripparticle.random, 0.1F, 0.9F));
         dripparticle.gravity = 0.005F;
         dripparticle.setColor(0.32F, 0.5F, 0.22F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class WaterFallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WaterFallProvider(SpriteSet p_428212_) {
         this.sprite = p_428212_;
      }

      public Particle createParticle(
         SimpleParticleType p_428147_,
         ClientLevel p_427103_,
         double p_422556_,
         double p_424038_,
         double p_427498_,
         double p_427098_,
         double p_430026_,
         double p_424612_,
         RandomSource p_424021_
      ) {
         DripParticle dripparticle = new DripParticle.FallAndLandParticle(
            p_427103_, p_422556_, p_424038_, p_427498_, Fluids.WATER, ParticleTypes.SPLASH, this.sprite.get(p_424021_)
         );
         dripparticle.setColor(0.2F, 0.3F, 1.0F);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class WaterHangProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WaterHangProvider(SpriteSet p_423520_) {
         this.sprite = p_423520_;
      }

      public Particle createParticle(
         SimpleParticleType p_431609_,
         ClientLevel p_429496_,
         double p_423746_,
         double p_425025_,
         double p_428783_,
         double p_426499_,
         double p_429875_,
         double p_423937_,
         RandomSource p_424319_
      ) {
         DripParticle dripparticle = new DripParticle.DripHangParticle(
            p_429496_, p_423746_, p_425025_, p_428783_, Fluids.WATER, ParticleTypes.FALLING_WATER, this.sprite.get(p_424319_)
         );
         dripparticle.setColor(0.2F, 0.3F, 1.0F);
         return dripparticle;
      }
   }
}
