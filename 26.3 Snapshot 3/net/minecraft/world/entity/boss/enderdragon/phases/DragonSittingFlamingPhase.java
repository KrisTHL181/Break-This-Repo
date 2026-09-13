package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonSittingFlamingPhase extends AbstractDragonSittingPhase {
   private static final int FLAME_DURATION = 200;
   private static final int SITTING_FLAME_ATTACKS_COUNT = 4;
   private static final int WARMUP_TIME = 10;
   private int flameTicks;
   private int flameCount;
   private @Nullable AreaEffectCloud flame;

   public DragonSittingFlamingPhase(final EnderDragon dragon) {
      super(dragon);
   }

   @Override
   public void doClientTick() {
      this.flameTicks++;
      if (this.flameTicks % 2 == 0 && this.flameTicks < 10) {
         Vec3 look = this.dragon.getHeadLookVector(1.0F).normalize();
         look.yRot((float) (-Math.PI / 4));
         double particleX = this.dragon.head.getX();
         double particleY = this.dragon.head.getY(0.5);
         double particleZ = this.dragon.head.getZ();

         for (int i = 0; i < 8; i++) {
            double px = particleX + this.dragon.getRandom().nextGaussian() / 2.0;
            double py = particleY + this.dragon.getRandom().nextGaussian() / 2.0;
            double pz = particleZ + this.dragon.getRandom().nextGaussian() / 2.0;

            for (int j = 0; j < 6; j++) {
               this.dragon
                  .level()
                  .addParticle(
                     PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), px, py, pz, -look.x * 0.08F * j, -look.y * 0.6F, -look.z * 0.08F * j
                  );
            }

            look.yRot((float) (Math.PI / 16));
         }
      }
   }

   @Override
   public void doServerTick(final ServerLevel level) {
      this.flameTicks++;
      if (this.flameTicks >= 200) {
         if (this.flameCount >= 4) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);
         } else {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
         }
      } else if (this.flameTicks == 10) {
         Vec3 look = new Vec3(this.dragon.head.getX() - this.dragon.getX(), 0.0, this.dragon.head.getZ() - this.dragon.getZ()).normalize();
         float radius = 5.0F;
         double x = this.dragon.head.getX() + look.x * 5.0 / 2.0;
         double z = this.dragon.head.getZ() + look.z * 5.0 / 2.0;
         double initialY = this.dragon.head.getY(0.5);
         double y = initialY;
         BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);

         while (level.isEmptyBlock(pos)) {
            if (--y < 0.0) {
               y = initialY;
               break;
            }

            pos.set(x, y, z);
         }

         y = Mth.floor(y) + 1;
         this.flame = new AreaEffectCloud(level, x, y, z);
         this.flame.setOwner(this.dragon);
         this.flame.setRadius(5.0F);
         this.flame.setDuration(200);
         this.flame.setCustomParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F));
         this.flame.setPotionDurationScale(0.25F);
         this.flame.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE));
         level.addFreshEntity(this.flame);
      }
   }

   @Override
   public void begin() {
      this.flameTicks = 0;
      this.flameCount++;
   }

   @Override
   public void end() {
      if (this.flame != null) {
         this.flame.discard();
         this.flame = null;
      }
   }

   @Override
   public EnderDragonPhase<DragonSittingFlamingPhase> getPhase() {
      return EnderDragonPhase.SITTING_FLAMING;
   }

   public void resetFlameCount() {
      this.flameCount = 0;
   }
}
