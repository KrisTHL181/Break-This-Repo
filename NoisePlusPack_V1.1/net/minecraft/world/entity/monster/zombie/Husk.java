package net.minecraft.world.entity.monster.zombie;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.camel.CamelHusk;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public class Husk extends Zombie {
   public Husk(EntityType<? extends Husk> p_459327_, Level p_457170_) {
      super(p_459327_, p_457170_);
   }

   @Override
   protected boolean isSunSensitive() {
      return false;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.HUSK_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_451770_) {
      return SoundEvents.HUSK_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.HUSK_DEATH;
   }

   @Override
   protected SoundEvent getStepSound() {
      return SoundEvents.HUSK_STEP;
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_458832_, Entity p_456248_) {
      boolean flag = super.doHurtTarget(p_458832_, p_456248_);
      if (flag && this.getMainHandItem().isEmpty() && p_456248_ instanceof LivingEntity) {
         float f = p_458832_.getCurrentDifficultyAt(this.blockPosition()).getEffectiveDifficulty();
         ((LivingEntity)p_456248_).addEffect(new MobEffectInstance(MobEffects.HUNGER, 140 * (int)f), this);
      }

      return flag;
   }

   @Override
   protected boolean convertsInWater() {
      return true;
   }

   @Override
   protected void doUnderWaterConversion(ServerLevel p_454240_) {
      this.convertToZombieType(p_454240_, EntityType.ZOMBIE);
      if (!this.isSilent()) {
         p_454240_.levelEvent(null, 1041, this.blockPosition(), 0);
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_455519_, DifficultyInstance p_457727_, EntitySpawnReason p_457019_, @Nullable SpawnGroupData p_455578_
   ) {
      RandomSource randomsource = p_455519_.getRandom();
      p_455578_ = super.finalizeSpawn(p_455519_, p_457727_, p_457019_, p_455578_);
      float f = p_457727_.getSpecialMultiplier();
      if (p_457019_ != EntitySpawnReason.CONVERSION) {
         this.setCanPickUpLoot(randomsource.nextFloat() < 0.55F * f);
      }

      if (p_455578_ != null) {
         p_455578_ = new Husk.HuskGroupData((Zombie.ZombieGroupData)p_455578_);
         ((Husk.HuskGroupData)p_455578_).triedToSpawnCamelHusk = p_457019_ != EntitySpawnReason.NATURAL;
      }

      if (p_455578_ instanceof Husk.HuskGroupData husk$huskgroupdata && !husk$huskgroupdata.triedToSpawnCamelHusk) {
         BlockPos blockpos = this.blockPosition();
         if (p_455519_.noCollision(EntityType.CAMEL_HUSK.getSpawnAABB(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5))) {
            husk$huskgroupdata.triedToSpawnCamelHusk = true;
            if (randomsource.nextFloat() < 0.1F) {
               this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
               CamelHusk camelhusk = EntityType.CAMEL_HUSK.create(this.level(), EntitySpawnReason.NATURAL);
               if (camelhusk != null) {
                  camelhusk.setPos(this.getX(), this.getY(), this.getZ());
                  camelhusk.finalizeSpawn(p_455519_, p_457727_, p_457019_, null);
                  this.startRiding(camelhusk, true, true);
                  p_455519_.addFreshEntity(camelhusk);
                  Parched parched = EntityType.PARCHED.create(this.level(), EntitySpawnReason.NATURAL);
                  if (parched != null) {
                     parched.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                     parched.finalizeSpawn(p_455519_, p_457727_, p_457019_, null);
                     parched.startRiding(camelhusk, false, false);
                     p_455519_.addFreshEntityWithPassengers(parched);
                  }
               }
            }
         }
      }

      return p_455578_;
   }

   public static class HuskGroupData extends Zombie.ZombieGroupData {
      public boolean triedToSpawnCamelHusk = false;

      public HuskGroupData(Zombie.ZombieGroupData p_454360_) {
         super(p_454360_.isBaby, p_454360_.canSpawnJockey);
      }
   }
}
