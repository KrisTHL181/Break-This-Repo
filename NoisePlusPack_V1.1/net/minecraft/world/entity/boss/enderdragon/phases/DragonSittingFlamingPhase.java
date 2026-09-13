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

   public DragonSittingFlamingPhase(EnderDragon p_31330_) {
      super(p_31330_);
   }

   @Override
   public void doClientTick() {
      this.flameTicks++;
      if (this.flameTicks % 2 == 0 && this.flameTicks < 10) {
         Vec3 vec3 = this.dragon.getHeadLookVector(1.0F).normalize();
         vec3.yRot((float) (-Math.PI / 4));
         double d0 = this.dragon.head.getX();
         double d1 = this.dragon.head.getY(0.5);
         double d2 = this.dragon.head.getZ();

         for (int i = 0; i < 8; i++) {
            double d3 = d0 + this.dragon.getRandom().nextGaussian() / 2.0;
            double d4 = d1 + this.dragon.getRandom().nextGaussian() / 2.0;
            double d5 = d2 + this.dragon.getRandom().nextGaussian() / 2.0;

            for (int j = 0; j < 6; j++) {
               this.dragon
                  .level()
                  .addParticle(
                     PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), d3, d4, d5, -vec3.x * 0.08F * j, -vec3.y * 0.6F, -vec3.z * 0.08F * j
                  );
            }

            vec3.yRot((float) (Math.PI / 16));
         }
      }
   }

   @Override
   public void doServerTick(ServerLevel p_368299_) {
      this.flameTicks++;
      if (this.flameTicks >= 200) {
         if (this.flameCount >= 4) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);
         } else {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
         }
      } else if (this.flameTicks == 10) {
         Vec3 vec3 = new Vec3(this.dragon.head.getX() - this.dragon.getX(), 0.0, this.dragon.head.getZ() - this.dragon.getZ()).normalize();
         float f = 5.0F;
         double d0 = this.dragon.head.getX() + vec3.x * 5.0 / 2.0;
         double d1 = this.dragon.head.getZ() + vec3.z * 5.0 / 2.0;
         double d2 = this.dragon.head.getY(0.5);
         double d3 = d2;
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(d0, d2, d1);

         while (p_368299_.isEmptyBlock(blockpos$mutableblockpos)) {
            if (--d3 < 0.0) {
               d3 = d2;
               break;
            }

            blockpos$mutableblockpos.set(d0, d3, d1);
         }

         d3 = Mth.floor(d3) + 1;
         this.flame = new AreaEffectCloud(p_368299_, d0, d3, d1);
         this.flame.setOwner(this.dragon);
         this.flame.setRadius(5.0F);
         this.flame.setDuration(200);
         this.flame.setCustomParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F));
         this.flame.setPotionDurationScale(0.25F);
         this.flame.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE));
         p_368299_.addFreshEntity(this.flame);
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
