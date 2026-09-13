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
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.camel.CamelHusk;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public class Husk extends Zombie {
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.49F, 0.98F)
      .withEyeHeight(0.825F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, 0.0F, 0.1875F, 0.0F));

   public Husk(final EntityType<? extends Husk> type, final Level level) {
      super(type, level);
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
   protected SoundEvent getHurtSound(final DamageSource source) {
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
   public boolean doHurtTarget(final ServerLevel level, final Entity target) {
      boolean result = super.doHurtTarget(level, target);
      if (result && this.getMainHandItem().isEmpty() && target instanceof LivingEntity livingEntity) {
         float difficulty = level.getCurrentDifficultyAt(this.blockPosition()).getEffectiveDifficulty();
         livingEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140 * (int)difficulty), this);
      }

      return result;
   }

   @Override
   protected boolean convertsInWater() {
      return true;
   }

   @Override
   protected void doUnderWaterConversion(final ServerLevel level) {
      this.convertToZombieType(level, EntityTypes.ZOMBIE);
      if (!this.isSilent()) {
         level.levelEvent(null, 1041, this.blockPosition(), 0);
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      RandomSource random = level.getRandom();
      groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
      float difficultyModifier = difficulty.getSpecialMultiplier();
      if (spawnReason != EntitySpawnReason.CONVERSION) {
         this.setCanPickUpLoot(random.nextFloat() < 0.55F * difficultyModifier);
      }

      if (groupData != null) {
         groupData = new Husk.HuskGroupData((Zombie.ZombieGroupData)groupData);
         ((Husk.HuskGroupData)groupData).triedToSpawnCamelHusk = spawnReason != EntitySpawnReason.NATURAL;
      }

      if (groupData instanceof Husk.HuskGroupData huskGroupData && !huskGroupData.triedToSpawnCamelHusk) {
         BlockPos pos = this.blockPosition();
         if (level.noCollision(EntityTypes.CAMEL_HUSK.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
            huskGroupData.triedToSpawnCamelHusk = true;
            if (random.nextFloat() < 0.1F) {
               this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
               CamelHusk camelHusk = EntityTypes.CAMEL_HUSK.create(this.level(), EntitySpawnReason.NATURAL);
               if (camelHusk != null) {
                  camelHusk.setPos(this.getX(), this.getY(), this.getZ());
                  camelHusk.finalizeSpawn(level, difficulty, spawnReason, null);
                  this.startRiding(camelHusk, true, true);
                  level.addFreshEntity(camelHusk);
                  Parched parched = EntityTypes.PARCHED.create(this.level(), EntitySpawnReason.NATURAL);
                  if (parched != null) {
                     parched.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                     parched.finalizeSpawn(level, difficulty, spawnReason, null);
                     parched.startRiding(camelHusk, false, false);
                     level.addFreshEntityWithPassengers(parched);
                  }
               }
            }
         }
      }

      return groupData;
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   public static class HuskGroupData extends Zombie.ZombieGroupData {
      public boolean triedToSpawnCamelHusk = false;

      public HuskGroupData(final Zombie.ZombieGroupData groupData) {
         super(groupData.isBaby, groupData.canSpawnJockey);
      }
   }
}
