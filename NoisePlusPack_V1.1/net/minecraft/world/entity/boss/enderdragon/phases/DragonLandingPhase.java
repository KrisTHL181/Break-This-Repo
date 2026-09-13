package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonLandingPhase extends AbstractDragonPhaseInstance {
   private @Nullable Vec3 targetLocation;

   public DragonLandingPhase(EnderDragon p_31305_) {
      super(p_31305_);
   }

   @Override
   public void doClientTick() {
      Vec3 vec3 = this.dragon.getHeadLookVector(1.0F).normalize();
      vec3.yRot((float) (-Math.PI / 4));
      double d0 = this.dragon.head.getX();
      double d1 = this.dragon.head.getY(0.5);
      double d2 = this.dragon.head.getZ();

      for (int i = 0; i < 8; i++) {
         RandomSource randomsource = this.dragon.getRandom();
         double d3 = d0 + randomsource.nextGaussian() / 2.0;
         double d4 = d1 + randomsource.nextGaussian() / 2.0;
         double d5 = d2 + randomsource.nextGaussian() / 2.0;
         Vec3 vec31 = this.dragon.getDeltaMovement();
         this.dragon
            .level()
            .addParticle(
               PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
               d3,
               d4,
               d5,
               -vec3.x * 0.08F + vec31.x,
               -vec3.y * 0.3F + vec31.y,
               -vec3.z * 0.08F + vec31.z
            );
         vec3.yRot((float) (Math.PI / 16));
      }
   }

   @Override
   public void doServerTick(ServerLevel p_361920_) {
      if (this.targetLocation == null) {
         this.targetLocation = Vec3.atBottomCenterOf(
            p_361920_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.dragon.getFightOrigin()))
         );
      }

      if (this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ()) < 1.0) {
         this.dragon.getPhaseManager().getPhase(EnderDragonPhase.SITTING_FLAMING).resetFlameCount();
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
      }
   }

   @Override
   public float getFlySpeed() {
      return 1.5F;
   }

   @Override
   public float getTurnSpeed() {
      float f = (float)this.dragon.getDeltaMovement().horizontalDistance() + 1.0F;
      float f1 = Math.min(f, 40.0F);
      return f1 / f;
   }

   @Override
   public void begin() {
      this.targetLocation = null;
   }

   @Override
   public @Nullable Vec3 getFlyTargetLocation() {
      return this.targetLocation;
   }

   @Override
   public EnderDragonPhase<DragonLandingPhase> getPhase() {
      return EnderDragonPhase.LANDING;
   }
}
