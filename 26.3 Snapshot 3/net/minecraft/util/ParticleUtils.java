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
   public static void spawnParticlesOnBlockFaces(final Level level, final BlockPos pos, final ParticleOptions particle, final IntProvider particlesPerFaceRange) {
      RandomSource random = level.getRandom();

      for (Direction direction : Direction.values()) {
         spawnParticlesOnBlockFace(level, pos, particle, particlesPerFaceRange, direction, () -> getRandomSpeedRanges(random), 0.55);
      }
   }

   public static void spawnParticlesOnBlockFace(
      final Level level,
      final BlockPos pos,
      final ParticleOptions particle,
      final IntProvider particlesPerFaceRange,
      final Direction face,
      final Supplier<Vec3> speedSupplier,
      final double stepFactor
   ) {
      int particleCount = particlesPerFaceRange.sample(level.getRandom());

      for (int i = 0; i < particleCount; i++) {
         spawnParticleOnFace(level, pos, face, particle, speedSupplier.get(), stepFactor);
      }
   }

   private static Vec3 getRandomSpeedRanges(final RandomSource random) {
      return new Vec3(Mth.nextDouble(random, -0.5, 0.5), Mth.nextDouble(random, -0.5, 0.5), Mth.nextDouble(random, -0.5, 0.5));
   }

   public static void spawnParticlesAlongAxis(
      final Direction.Axis attachedAxis,
      final Level level,
      final BlockPos pos,
      final double radius,
      final ParticleOptions particle,
      final UniformInt sparkCount
   ) {
      Vec3 centerOfBlock = Vec3.atCenterOf(pos);
      boolean stepX = attachedAxis == Direction.Axis.X;
      boolean stepY = attachedAxis == Direction.Axis.Y;
      boolean stepZ = attachedAxis == Direction.Axis.Z;
      RandomSource random = level.getRandom();
      int particleCount = sparkCount.sample(random);

      for (int i = 0; i < particleCount; i++) {
         double x = centerOfBlock.x + Mth.nextDouble(random, -1.0, 1.0) * (stepX ? 0.5 : radius);
         double y = centerOfBlock.y + Mth.nextDouble(random, -1.0, 1.0) * (stepY ? 0.5 : radius);
         double z = centerOfBlock.z + Mth.nextDouble(random, -1.0, 1.0) * (stepZ ? 0.5 : radius);
         double xBaseSpeed = stepX ? Mth.nextDouble(random, -1.0, 1.0) : 0.0;
         double yBaseSpeed = stepY ? Mth.nextDouble(random, -1.0, 1.0) : 0.0;
         double zBaseSpeed = stepZ ? Mth.nextDouble(random, -1.0, 1.0) : 0.0;
         level.addParticle(particle, x, y, z, xBaseSpeed, yBaseSpeed, zBaseSpeed);
      }
   }

   public static void spawnParticleOnFace(
      final Level level, final BlockPos pos, final Direction face, final ParticleOptions particle, final Vec3 speed, final double stepFactor
   ) {
      Vec3 centerOfBlock = Vec3.atCenterOf(pos);
      int stepX = face.getStepX();
      int stepY = face.getStepY();
      int stepZ = face.getStepZ();
      RandomSource random = level.getRandom();
      double x = centerOfBlock.x + (stepX == 0 ? Mth.nextDouble(random, -0.5, 0.5) : stepX * stepFactor);
      double y = centerOfBlock.y + (stepY == 0 ? Mth.nextDouble(random, -0.5, 0.5) : stepY * stepFactor);
      double z = centerOfBlock.z + (stepZ == 0 ? Mth.nextDouble(random, -0.5, 0.5) : stepZ * stepFactor);
      double xBaseSpeed = stepX == 0 ? speed.x() : 0.0;
      double yBaseSpeed = stepY == 0 ? speed.y() : 0.0;
      double zBaseSpeed = stepZ == 0 ? speed.z() : 0.0;
      level.addParticle(particle, x, y, z, xBaseSpeed, yBaseSpeed, zBaseSpeed);
   }

   public static void spawnParticleBelow(final Level level, final BlockPos pos, final RandomSource random, final ParticleOptions particle) {
      double x = pos.getX() + random.nextDouble();
      double y = pos.getY() - 0.05;
      double z = pos.getZ() + random.nextDouble();
      level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
   }

   public static void spawnParticleInBlock(final LevelAccessor level, final BlockPos pos, final int count, final ParticleOptions particle) {
      double spreadWidth = 0.5;
      BlockState blockState = level.getBlockState(pos);
      double spreadHeight = blockState.isAir() ? 1.0 : blockState.getShape(level, pos).max(Direction.Axis.Y);
      spawnParticles(level, pos, count, 0.5, spreadHeight, true, particle);
   }

   public static void spawnParticles(
      final LevelAccessor level,
      final BlockPos pos,
      final int count,
      final double spreadWidth,
      final double spreadHeight,
      final boolean allowFloatingParticles,
      final ParticleOptions particle
   ) {
      RandomSource random = level.getRandom();

      for (int i = 0; i < count; i++) {
         double xVelocity = random.nextGaussian() * 0.02;
         double yVelocity = random.nextGaussian() * 0.02;
         double zVelocity = random.nextGaussian() * 0.02;
         double spreadStartOffset = 0.5 - spreadWidth;
         double x = pos.getX() + spreadStartOffset + random.nextDouble() * spreadWidth * 2.0;
         double y = pos.getY() + random.nextDouble() * spreadHeight;
         double z = pos.getZ() + spreadStartOffset + random.nextDouble() * spreadWidth * 2.0;
         if (allowFloatingParticles || !level.getBlockState(BlockPos.containing(x, y, z).below()).isAir()) {
            level.addParticle(particle, x, y, z, xVelocity, yVelocity, zVelocity);
         }
      }
   }

   public static void spawnSmashAttackParticles(final LevelAccessor level, final BlockPos pos, final int count) {
      Vec3 center = Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0);
      BlockParticleOption particle = new BlockParticleOption(ParticleTypes.DUST_PILLAR, level.getBlockState(pos));

      for (int i = 0; i < count / 3.0F; i++) {
         double x = center.x + level.getRandom().nextGaussian() / 2.0;
         double y = center.y;
         double z = center.z + level.getRandom().nextGaussian() / 2.0;
         double xd = level.getRandom().nextGaussian() * 0.2F;
         double yd = level.getRandom().nextGaussian() * 0.2F;
         double zd = level.getRandom().nextGaussian() * 0.2F;
         level.addParticle(particle, x, y, z, xd, yd, zd);
      }

      for (int i = 0; i < count / 1.5F; i++) {
         double x = center.x + 3.5 * Math.cos(i) + level.getRandom().nextGaussian() / 2.0;
         double y = center.y;
         double z = center.z + 3.5 * Math.sin(i) + level.getRandom().nextGaussian() / 2.0;
         double xd = level.getRandom().nextGaussian() * 0.05F;
         double yd = level.getRandom().nextGaussian() * 0.05F;
         double zd = level.getRandom().nextGaussian() * 0.05F;
         level.addParticle(particle, x, y, z, xd, yd, zd);
      }
   }
}
