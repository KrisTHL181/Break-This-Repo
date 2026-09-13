package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.phys.Vec3;

public class ClientExplosionTracker {
   private static final int MAX_PARTICLES_PER_TICK = 512;
   private final List<ClientExplosionTracker.ExplosionInfo> explosions = new ArrayList<>();

   public void track(final Vec3 center, final float radius, final int blockCount, final WeightedList<ExplosionParticleInfo> blockParticles) {
      if (!blockParticles.isEmpty()) {
         this.explosions.add(new ClientExplosionTracker.ExplosionInfo(center, radius, blockCount, blockParticles));
      }
   }

   public void tick(final ClientLevel level) {
      if (Minecraft.getInstance().options.particles().get() != ParticleStatus.ALL) {
         this.explosions.clear();
      } else {
         int totalBlocks = WeightedRandom.getTotalWeight(this.explosions, ClientExplosionTracker.ExplosionInfo::blockCount);
         int totalParticles = Math.min(totalBlocks, 512);

         for (int i = 0; i < totalParticles; i++) {
            WeightedRandom.getRandomItem(level.getRandom(), this.explosions, totalBlocks, ClientExplosionTracker.ExplosionInfo::blockCount)
               .ifPresent(info -> this.addParticle(level, info));
         }

         this.explosions.clear();
      }
   }

   private void addParticle(final ClientLevel level, final ClientExplosionTracker.ExplosionInfo explosion) {
      RandomSource random = level.getRandom();
      Vec3 center = explosion.center();
      Vec3 directionFromCenter = new Vec3(random.nextFloat() * 2.0F - 1.0F, random.nextFloat() * 2.0F - 1.0F, random.nextFloat() * 2.0F - 1.0F).normalize();
      float radius = (float)Math.cbrt(random.nextFloat()) * explosion.radius();
      Vec3 localPos = directionFromCenter.scale(radius);
      Vec3 pos = center.add(localPos);
      if (level.getBlockState(BlockPos.containing(pos)).isAir()) {
         float speed = 0.5F / (radius / explosion.radius() + 0.1F) * random.nextFloat() * random.nextFloat() + 0.3F;
         ExplosionParticleInfo info = explosion.blockParticles.getRandomOrThrow(random);
         Vec3 particlePos = center.add(localPos.scale(info.scaling()));
         Vec3 particleVelocity = directionFromCenter.scale(speed * info.speed());
         level.addParticle(info.particle(), particlePos.x(), particlePos.y(), particlePos.z(), particleVelocity.x(), particleVelocity.y(), particleVelocity.z());
      }
   }

   private record ExplosionInfo(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles) {
   }
}
