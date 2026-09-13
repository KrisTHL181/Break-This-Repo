package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class EvokerFangs extends Entity implements TraceableEntity {
   public static final int ATTACK_DURATION = 20;
   public static final int LIFE_OFFSET = 2;
   public static final int ATTACK_TRIGGER_TICKS = 14;
   private static final int DEFAULT_WARMUP_DELAY = 0;
   private int warmupDelayTicks = 0;
   private boolean sentSpikeEvent;
   private int lifeTicks = 22;
   private boolean clientSideAttackStarted;
   private @Nullable EntityReference<LivingEntity> owner;

   public EvokerFangs(final EntityType<? extends EvokerFangs> type, final Level level) {
      super(type, level);
   }

   public EvokerFangs(
      final Level level, final double x, final double y, final double z, final float rotaionRadians, final int warmupDelayTicks, final LivingEntity owner
   ) {
      this(EntityTypes.EVOKER_FANGS, level);
      this.warmupDelayTicks = warmupDelayTicks;
      this.setOwner(owner);
      this.setYRot(rotaionRadians * (180.0F / (float)Math.PI));
      this.setPos(x, y, z);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
   }

   public void setOwner(final @Nullable LivingEntity owner) {
      this.owner = EntityReference.of(owner);
   }

   public @Nullable LivingEntity getOwner() {
      return EntityReference.getLivingEntity(this.owner, this.level());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      this.warmupDelayTicks = input.getIntOr("Warmup", 0);
      this.owner = EntityReference.read(input, "Owner");
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      output.putInt("Warmup", this.warmupDelayTicks);
      EntityReference.store(this.owner, output, "Owner");
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         if (this.clientSideAttackStarted) {
            this.lifeTicks--;
            if (this.lifeTicks == 14) {
               for (int i = 0; i < 12; i++) {
                  double x = this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
                  double y = this.getY() + 0.05 + this.random.nextDouble();
                  double z = this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
                  double xd = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                  double yd = 0.3 + this.random.nextDouble() * 0.3;
                  double zd = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                  this.level().addParticle(ParticleTypes.CRIT, x, y + 1.0, z, xd, yd, zd);
               }
            }
         }
      } else if (--this.warmupDelayTicks < 0) {
         if (this.warmupDelayTicks == -8) {
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2, 0.0, 0.2))) {
               this.dealDamageTo(entity);
            }
         }

         if (!this.sentSpikeEvent) {
            this.level().broadcastEntityEvent(this, (byte)4);
            this.sentSpikeEvent = true;
         }

         if (--this.lifeTicks < 0) {
            this.discard();
         }
      }
   }

   private void dealDamageTo(final LivingEntity entity) {
      LivingEntity currentOwner = this.getOwner();
      if (entity.isAlive() && !entity.isInvulnerable() && entity != currentOwner) {
         if (currentOwner == null) {
            entity.hurt(this.damageSources().magic(), 6.0F);
         } else {
            if (currentOwner.isAlliedTo(entity)) {
               return;
            }

            DamageSource damageSource = this.damageSources().indirectMagic(this, currentOwner);
            if (this.level() instanceof ServerLevel serverLevel && entity.hurtServer(serverLevel, damageSource, 6.0F)) {
               EnchantmentHelper.doPostAttackEffects(serverLevel, entity, damageSource);
            }
         }
      }
   }

   @Override
   public void handleEntityEvent(final byte id) {
      super.handleEntityEvent(id);
      if (id == 4) {
         this.clientSideAttackStarted = true;
         if (!this.isSilent()) {
            this.level()
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.EVOKER_FANGS_ATTACK,
                  this.getSoundSource(),
                  1.0F,
                  this.random.nextFloat() * 0.2F + 0.85F,
                  false
               );
         }
      }
   }

   public float getAnimationProgress(final float a) {
      if (!this.clientSideAttackStarted) {
         return 0.0F;
      }

      int remainingLife = this.lifeTicks - 2;
      return remainingLife <= 0 ? 1.0F : 1.0F - (remainingLife - a) / 20.0F;
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return false;
   }
}
