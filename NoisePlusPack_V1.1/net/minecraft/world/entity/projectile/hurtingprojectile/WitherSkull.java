package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WitherSkull extends AbstractHurtingProjectile {
   private static final EntityDataAccessor<Boolean> DATA_DANGEROUS = SynchedEntityData.defineId(WitherSkull.class, EntityDataSerializers.BOOLEAN);
   private static final boolean DEFAULT_DANGEROUS = false;

   public WitherSkull(EntityType<? extends WitherSkull> p_460972_, Level p_451016_) {
      super(p_460972_, p_451016_);
   }

   public WitherSkull(Level p_459998_, LivingEntity p_452438_, Vec3 p_459911_) {
      super(EntityType.WITHER_SKULL, p_452438_, p_459911_, p_459998_);
   }

   @Override
   protected float getInertia() {
      return this.isDangerous() ? 0.73F : super.getInertia();
   }

   @Override
   public boolean isOnFire() {
      return false;
   }

   @Override
   public float getBlockExplosionResistance(
      Explosion p_459983_, BlockGetter p_451795_, BlockPos p_456538_, BlockState p_459565_, FluidState p_454710_, float p_453616_
   ) {
      return this.isDangerous() && WitherBoss.canDestroy(p_459565_) ? Math.min(0.8F, p_453616_) : p_453616_;
   }

   @Override
   protected void onHitEntity(EntityHitResult p_452288_) {
      super.onHitEntity(p_452288_);
      if (this.level() instanceof ServerLevel serverlevel) {
         Entity entity = p_452288_.getEntity();
         boolean flag;
         if (this.getOwner() instanceof LivingEntity livingentity) {
            DamageSource damagesource = this.damageSources().witherSkull(this, livingentity);
            flag = entity.hurtServer(serverlevel, damagesource, 8.0F);
            if (flag) {
               if (entity.isAlive()) {
                  EnchantmentHelper.doPostAttackEffects(serverlevel, entity, damagesource);
               } else {
                  livingentity.heal(5.0F);
               }
            }
         } else {
            flag = entity.hurtServer(serverlevel, this.damageSources().magic(), 5.0F);
         }

         if (flag && entity instanceof LivingEntity livingentity1) {
            int i = 0;
            if (this.level().getDifficulty() == Difficulty.NORMAL) {
               i = 10;
            } else if (this.level().getDifficulty() == Difficulty.HARD) {
               i = 40;
            }

            if (i > 0) {
               livingentity1.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * i, 1), this.getEffectSource());
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult p_452878_) {
      super.onHit(p_452878_);
      if (!this.level().isClientSide()) {
         this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.0F, false, Level.ExplosionInteraction.MOB);
         this.discard();
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_460919_) {
      p_460919_.define(DATA_DANGEROUS, false);
   }

   public boolean isDangerous() {
      return this.entityData.get(DATA_DANGEROUS);
   }

   public void setDangerous(boolean p_455380_) {
      this.entityData.set(DATA_DANGEROUS, p_455380_);
   }

   @Override
   protected boolean shouldBurn() {
      return false;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_452289_) {
      super.addAdditionalSaveData(p_452289_);
      p_452289_.putBoolean("dangerous", this.isDangerous());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_460994_) {
      super.readAdditionalSaveData(p_460994_);
      this.setDangerous(p_460994_.getBooleanOr("dangerous", false));
   }
}
