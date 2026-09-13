package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TerrainParticle extends SingleQuadParticle {
   private final SingleQuadParticle.Layer layer;
   private final BlockPos pos;
   private final float uo;
   private final float vo;

   public TerrainParticle(
      ClientLevel p_108282_, double p_108283_, double p_108284_, double p_108285_, double p_108286_, double p_108287_, double p_108288_, BlockState p_108289_
   ) {
      this(p_108282_, p_108283_, p_108284_, p_108285_, p_108286_, p_108287_, p_108288_, p_108289_, BlockPos.containing(p_108283_, p_108284_, p_108285_));
   }

   public TerrainParticle(
      ClientLevel p_172451_,
      double p_172452_,
      double p_172453_,
      double p_172454_,
      double p_172455_,
      double p_172456_,
      double p_172457_,
      BlockState p_172458_,
      BlockPos p_172459_
   ) {
      super(
         p_172451_,
         p_172452_,
         p_172453_,
         p_172454_,
         p_172455_,
         p_172456_,
         p_172457_,
         Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(p_172458_)
      );
      this.pos = p_172459_;
      this.gravity = 1.0F;
      this.rCol = 0.6F;
      this.gCol = 0.6F;
      this.bCol = 0.6F;
      if (!p_172458_.is(Blocks.GRASS_BLOCK)) {
         int i = Minecraft.getInstance().getBlockColors().getColor(p_172458_, p_172451_, p_172459_, 0);
         this.rCol *= (i >> 16 & 0xFF) / 255.0F;
         this.gCol *= (i >> 8 & 0xFF) / 255.0F;
         this.bCol *= (i & 0xFF) / 255.0F;
      }

      this.quadSize /= 2.0F;
      this.uo = this.random.nextFloat() * 3.0F;
      this.vo = this.random.nextFloat() * 3.0F;
      this.layer = this.sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS) ? SingleQuadParticle.Layer.TERRAIN : SingleQuadParticle.Layer.ITEMS;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return this.layer;
   }

   @Override
   protected float getU0() {
      return this.sprite.getU((this.uo + 1.0F) / 4.0F);
   }

   @Override
   protected float getU1() {
      return this.sprite.getU(this.uo / 4.0F);
   }

   @Override
   protected float getV0() {
      return this.sprite.getV(this.vo / 4.0F);
   }

   @Override
   protected float getV1() {
      return this.sprite.getV((this.vo + 1.0F) / 4.0F);
   }

   @Override
   public int getLightColor(float p_108291_) {
      int i = super.getLightColor(p_108291_);
      return i == 0 && this.level.hasChunkAt(this.pos) ? LevelRenderer.getLightColor(this.level, this.pos) : i;
   }

   static @Nullable TerrainParticle createTerrainParticle(
      BlockParticleOption p_331600_,
      ClientLevel p_334810_,
      double p_328897_,
      double p_329583_,
      double p_331123_,
      double p_333546_,
      double p_335782_,
      double p_335068_
   ) {
      BlockState blockstate = p_331600_.getState();
      return !blockstate.isAir() && !blockstate.is(Blocks.MOVING_PISTON) && blockstate.shouldSpawnTerrainParticles()
         ? new TerrainParticle(p_334810_, p_328897_, p_329583_, p_331123_, p_333546_, p_335782_, p_335068_, blockstate)
         : null;
   }

   @OnlyIn(Dist.CLIENT)
   public static class CrumblingProvider implements ParticleProvider<BlockParticleOption> {
      public @Nullable Particle createParticle(
         BlockParticleOption p_429437_,
         ClientLevel p_360938_,
         double p_363908_,
         double p_364110_,
         double p_367420_,
         double p_361226_,
         double p_368425_,
         double p_369835_,
         RandomSource p_426413_
      ) {
         Particle particle = TerrainParticle.createTerrainParticle(p_429437_, p_360938_, p_363908_, p_364110_, p_367420_, p_361226_, p_368425_, p_369835_);
         if (particle != null) {
            particle.setParticleSpeed(0.0, 0.0, 0.0);
            particle.setLifetime(p_426413_.nextInt(10) + 1);
         }

         return particle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DustPillarProvider implements ParticleProvider<BlockParticleOption> {
      public @Nullable Particle createParticle(
         BlockParticleOption p_425026_,
         ClientLevel p_333584_,
         double p_334734_,
         double p_330071_,
         double p_331620_,
         double p_327843_,
         double p_334896_,
         double p_333489_,
         RandomSource p_425441_
      ) {
         Particle particle = TerrainParticle.createTerrainParticle(p_425026_, p_333584_, p_334734_, p_330071_, p_331620_, p_327843_, p_334896_, p_333489_);
         if (particle != null) {
            particle.setParticleSpeed(p_425441_.nextGaussian() / 30.0, p_334896_ + p_425441_.nextGaussian() / 2.0, p_425441_.nextGaussian() / 30.0);
            particle.setLifetime(p_425441_.nextInt(20) + 20);
         }

         return particle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<BlockParticleOption> {
      public @Nullable Particle createParticle(
         BlockParticleOption p_108304_,
         ClientLevel p_108305_,
         double p_108306_,
         double p_108307_,
         double p_108308_,
         double p_108309_,
         double p_108310_,
         double p_108311_,
         RandomSource p_425691_
      ) {
         return TerrainParticle.createTerrainParticle(p_108304_, p_108305_, p_108306_, p_108307_, p_108308_, p_108309_, p_108310_, p_108311_);
      }
   }
}
