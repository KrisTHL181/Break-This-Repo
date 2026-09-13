package net.minecraft.util;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ParticleUtils {
   public static void spawnParticlesOnBlockFaces(Level p_216314_, BlockPos p_216315_, ParticleOptions p_216316_, IntProvider p_216317_) {
      for (Direction direction : Direction.values()) {
         spawnParticlesOnBlockFace(p_216314_, p_216315_, p_216316_, p_216317_, direction, () -> getRandomSpeedRanges(p_216314_.random), 0.55);
      }
   }

   public static void spawnParticlesOnBlockFace(
      Level p_216319_, BlockPos p_216320_, ParticleOptions p_216321_, IntProvider p_216322_, Direction p_216323_, Supplier<Vec3> p_216324_, double p_216325_
   ) {
      int i = p_216322_.sample(p_216319_.random);

      for (int j = 0; j < i; j++) {
         spawnParticleOnFace(p_216319_, p_216320_, p_216323_, p_216321_, p_216324_.get(), p_216325_);
      }
   }

   private static Vec3 getRandomSpeedRanges(RandomSource p_216303_) {
      return new Vec3(Mth.nextDouble(p_216303_, -0.5, 0.5), Mth.nextDouble(p_216303_, -0.5, 0.5), Mth.nextDouble(p_216303_, -0.5, 0.5));
   }

   public static void spawnParticlesAlongAxis(
      Direction.Axis p_144968_, Level p_144969_, BlockPos p_144970_, double p_144971_, ParticleOptions p_144972_, UniformInt p_144973_
   ) {
      Vec3 vec3 = Vec3.atCenterOf(p_144970_);
      boolean flag = p_144968_ == Direction.Axis.X;
      boolean flag1 = p_144968_ == Direction.Axis.Y;
      boolean flag2 = p_144968_ == Direction.Axis.Z;
      int i = p_144973_.sample(p_144969_.random);

      for (int j = 0; j < i; j++) {
         double d0 = vec3.x + Mth.nextDouble(p_144969_.random, -1.0, 1.0) * (flag ? 0.5 : p_144971_);
         double d1 = vec3.y + Mth.nextDouble(p_144969_.random, -1.0, 1.0) * (flag1 ? 0.5 : p_144971_);
         double d2 = vec3.z + Mth.nextDouble(p_144969_.random, -1.0, 1.0) * (flag2 ? 0.5 : p_144971_);
         double d3 = flag ? Mth.nextDouble(p_144969_.random, -1.0, 1.0) : 0.0;
         double d4 = flag1 ? Mth.nextDouble(p_144969_.random, -1.0, 1.0) : 0.0;
         double d5 = flag2 ? Mth.nextDouble(p_144969_.random, -1.0, 1.0) : 0.0;
         p_144969_.addParticle(p_144972_, d0, d1, d2, d3, d4, d5);
      }
   }

   public static void spawnParticleOnFace(Level p_216307_, BlockPos p_216308_, Direction p_216309_, ParticleOptions p_216310_, Vec3 p_216311_, double p_216312_) {
      Vec3 vec3 = Vec3.atCenterOf(p_216308_);
      int i = p_216309_.getStepX();
      int j = p_216309_.getStepY();
      int k = p_216309_.getStepZ();
      double d0 = vec3.x + (i == 0 ? Mth.nextDouble(p_216307_.random, -0.5, 0.5) : i * p_216312_);
      double d1 = vec3.y + (j == 0 ? Mth.nextDouble(p_216307_.random, -0.5, 0.5) : j * p_216312_);
      double d2 = vec3.z + (k == 0 ? Mth.nextDouble(p_216307_.random, -0.5, 0.5) : k * p_216312_);
      double d3 = i == 0 ? p_216311_.x() : 0.0;
      double d4 = j == 0 ? p_216311_.y() : 0.0;
      double d5 = k == 0 ? p_216311_.z() : 0.0;
      p_216307_.addParticle(p_216310_, d0, d1, d2, d3, d4, d5);
   }

   public static void spawnParticleBelow(Level p_273159_, BlockPos p_273452_, RandomSource p_273538_, ParticleOptions p_273419_) {
      double d0 = p_273452_.getX() + p_273538_.nextDouble();
      double d1 = p_273452_.getY() - 0.05;
      double d2 = p_273452_.getZ() + p_273538_.nextDouble();
      p_273159_.addParticle(p_273419_, d0, d1, d2, 0.0, 0.0, 0.0);
   }

   public static void spawnParticleInBlock(LevelAccessor p_335531_, BlockPos p_329785_, int p_335673_, ParticleOptions p_330338_) {
      double d0 = 0.5;
      BlockState blockstate = p_335531_.getBlockState(p_329785_);
      double d1 = blockstate.isAir() ? 1.0 : blockstate.getShape(p_335531_, p_329785_).max(Direction.Axis.Y);
      spawnParticles(p_335531_, p_329785_, p_335673_, 0.5, d1, true, p_330338_);
   }

   public static void spawnParticles(
      LevelAccessor p_332146_, BlockPos p_333994_, int p_332880_, double p_335286_, double p_334021_, boolean p_328793_, ParticleOptions p_329517_
   ) {
      RandomSource randomsource = p_332146_.getRandom();

      for (int i = 0; i < p_332880_; i++) {
         double d0 = randomsource.nextGaussian() * 0.02;
         double d1 = randomsource.nextGaussian() * 0.02;
         double d2 = randomsource.nextGaussian() * 0.02;
         double d3 = 0.5 - p_335286_;
         double d4 = p_333994_.getX() + d3 + randomsource.nextDouble() * p_335286_ * 2.0;
         double d5 = p_333994_.getY() + randomsource.nextDouble() * p_334021_;
         double d6 = p_333994_.getZ() + d3 + randomsource.nextDouble() * p_335286_ * 2.0;
         if (p_328793_ || !p_332146_.getBlockState(BlockPos.containing(d4, d5, d6).below()).isAir()) {
            p_332146_.addParticle(p_329517_, d4, d5, d6, d0, d1, d2);
         }
      }
   }

   public static void spawnSmashAttackParticles(LevelAccessor p_333323_, BlockPos p_331250_, int p_329230_) {
      Vec3 vec3 = p_331250_.getCenter().add(0.0, 0.5, 0.0);
      BlockParticleOption blockparticleoption = new BlockParticleOption(ParticleTypes.DUST_PILLAR, p_333323_.getBlockState(p_331250_));

      for (int i = 0; i < p_329230_ / 3.0F; i++) {
         double d0 = vec3.x + p_333323_.getRandom().nextGaussian() / 2.0;
         double d1 = vec3.y;
         double d2 = vec3.z + p_333323_.getRandom().nextGaussian() / 2.0;
         double d3 = p_333323_.getRandom().nextGaussian() * 0.2F;
         double d4 = p_333323_.getRandom().nextGaussian() * 0.2F;
         double d5 = p_333323_.getRandom().nextGaussian() * 0.2F;
         p_333323_.addParticle(blockparticleoption, d0, d1, d2, d3, d4, d5);
      }

      for (int j = 0; j < p_329230_ / 1.5F; j++) {
         double d6 = vec3.x + 3.5 * Math.cos(j) + p_333323_.getRandom().nextGaussian() / 2.0;
         double d7 = vec3.y;
         double d8 = vec3.z + 3.5 * Math.sin(j) + p_333323_.getRandom().nextGaussian() / 2.0;
         double d9 = p_333323_.getRandom().nextGaussian() * 0.05F;
         double d10 = p_333323_.getRandom().nextGaussian() * 0.05F;
         double d11 = p_333323_.getRandom().nextGaussian() * 0.05F;
         p_333323_.addParticle(blockparticleoption, d6, d7, d8, d9, d10, d11);
      }
   }
}
