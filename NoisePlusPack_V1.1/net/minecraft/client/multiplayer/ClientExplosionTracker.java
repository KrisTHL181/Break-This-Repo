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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientExplosionTracker {
   private static final int MAX_PARTICLES_PER_TICK = 512;
   private final List<ClientExplosionTracker.ExplosionInfo> explosions = new ArrayList<>();

   public void track(Vec3 p_429764_, float p_422326_, int p_422678_, WeightedList<ExplosionParticleInfo> p_424396_) {
      if (!p_424396_.isEmpty()) {
         this.explosions.add(new ClientExplosionTracker.ExplosionInfo(p_429764_, p_422326_, p_422678_, p_424396_));
      }
   }

   public void tick(ClientLevel p_424030_) {
      if (Minecraft.getInstance().options.particles().get() != ParticleStatus.ALL) {
         this.explosions.clear();
      } else {
         int i = WeightedRandom.getTotalWeight(this.explosions, ClientExplosionTracker.ExplosionInfo::blockCount);
         int j = Math.min(i, 512);

         for (int k = 0; k < j; k++) {
            WeightedRandom.getRandomItem(p_424030_.getRandom(), this.explosions, i, ClientExplosionTracker.ExplosionInfo::blockCount)
               .ifPresent(p_423158_ -> this.addParticle(p_424030_, p_423158_));
         }

         this.explosions.clear();
      }
   }

   private void addParticle(ClientLevel p_424756_, ClientExplosionTracker.ExplosionInfo p_423042_) {
      RandomSource randomsource = p_424756_.getRandom();
      Vec3 vec3 = p_423042_.center();
      Vec3 vec31 = new Vec3(randomsource.nextFloat() * 2.0F - 1.0F, randomsource.nextFloat() * 2.0F - 1.0F, randomsource.nextFloat() * 2.0F - 1.0F).normalize();
      float f = (float)Math.cbrt(randomsource.nextFloat()) * p_423042_.radius();
      Vec3 vec32 = vec31.scale(f);
      Vec3 vec33 = vec3.add(vec32);
      if (p_424756_.getBlockState(BlockPos.containing(vec33)).isAir()) {
         float f1 = 0.5F / (f / p_423042_.radius() + 0.1F) * randomsource.nextFloat() * randomsource.nextFloat() + 0.3F;
         ExplosionParticleInfo explosionparticleinfo = p_423042_.blockParticles.getRandomOrThrow(randomsource);
         Vec3 vec34 = vec3.add(vec32.scale(explosionparticleinfo.scaling()));
         Vec3 vec35 = vec31.scale(f1 * explosionparticleinfo.speed());
         p_424756_.addParticle(explosionparticleinfo.particle(), vec34.x(), vec34.y(), vec34.z(), vec35.x(), vec35.y(), vec35.z());
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ExplosionInfo(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles) {
   }
}
