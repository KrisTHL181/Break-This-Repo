package net.minecraft.world.entity.animal.equine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SkeletonHorse extends AbstractHorse {
   private final SkeletonTrapGoal skeletonTrapGoal = new SkeletonTrapGoal(this);
   private static final int TRAP_MAX_LIFE = 18000;
   private static final boolean DEFAULT_IS_TRAP = false;
   private static final int DEFAULT_TRAP_TIME = 0;
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.SKELETON_HORSE
      .getDimensions()
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityType.SKELETON_HORSE.getHeight() - 0.03125F, 0.0F))
      .scale(0.5F);
   private boolean isTrap = false;
   private int trapTime = 0;

   public SkeletonHorse(EntityType<? extends SkeletonHorse> p_455299_, Level p_457668_) {
      super(p_455299_, p_457668_);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 15.0).add(Attributes.MOVEMENT_SPEED, 0.2F);
   }

   public static boolean checkSkeletonHorseSpawnRules(
      EntityType<? extends Animal> p_457544_, LevelAccessor p_453324_, EntitySpawnReason p_456279_, BlockPos p_452689_, RandomSource p_453523_
   ) {
      return !EntitySpawnReason.isSpawner(p_456279_)
         ? Animal.checkAnimalSpawnRules(p_457544_, p_453324_, p_456279_, p_452689_, p_453523_)
         : EntitySpawnReason.ignoresLightRequirements(p_456279_) || isBrightEnoughToSpawn(p_453324_, p_452689_);
   }

   @Override
   protected void randomizeAttributes(RandomSource p_459150_) {
      this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(p_459150_::nextDouble));
   }

   @Override
   protected void addBehaviourGoals() {
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isEyeInFluid(FluidTags.WATER) ? SoundEvents.SKELETON_HORSE_AMBIENT_WATER : SoundEvents.SKELETON_HORSE_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SKELETON_HORSE_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_454066_) {
      return SoundEvents.SKELETON_HORSE_HURT;
   }

   @Override
   protected SoundEvent getSwimSound() {
      if (this.onGround()) {
         if (!this.isVehicle()) {
            return SoundEvents.SKELETON_HORSE_STEP_WATER;
         }

         this.gallopSoundCounter++;
         if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
            return SoundEvents.SKELETON_HORSE_GALLOP_WATER;
         }

         if (this.gallopSoundCounter <= 5) {
            return SoundEvents.SKELETON_HORSE_STEP_WATER;
         }
      }

      return SoundEvents.SKELETON_HORSE_SWIM;
   }

   @Override
   protected void playSwimSound(float p_457105_) {
      if (this.onGround()) {
         super.playSwimSound(0.3F);
      } else {
         super.playSwimSound(Math.min(0.1F, p_457105_ * 25.0F));
      }
   }

   @Override
   protected void playJumpSound() {
      if (this.isInWater()) {
         this.playSound(SoundEvents.SKELETON_HORSE_JUMP_WATER, 0.4F, 1.0F);
      } else {
         super.playJumpSound();
      }
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_450802_) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_450802_);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.isTrap() && this.trapTime++ >= 18000) {
         this.discard();
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_458576_) {
      super.addAdditionalSaveData(p_458576_);
      p_458576_.putBoolean("SkeletonTrap", this.isTrap());
      p_458576_.putInt("SkeletonTrapTime", this.trapTime);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_457163_) {
      super.readAdditionalSaveData(p_457163_);
      this.setTrap(p_457163_.getBooleanOr("SkeletonTrap", false));
      this.trapTime = p_457163_.getIntOr("SkeletonTrapTime", 0);
   }

   @Override
   protected float getWaterSlowDown() {
      return 0.96F;
   }

   public boolean isTrap() {
      return this.isTrap;
   }

   public void setTrap(boolean p_451433_) {
      if (p_451433_ != this.isTrap) {
         this.isTrap = p_451433_;
         if (p_451433_) {
            this.goalSelector.addGoal(1, this.skeletonTrapGoal);
         } else {
            this.goalSelector.removeGoal(this.skeletonTrapGoal);
         }
      }
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_455079_, AgeableMob p_460645_) {
      return EntityType.SKELETON_HORSE.create(p_455079_, EntitySpawnReason.BREEDING);
   }

   @Override
   public InteractionResult mobInteract(Player p_460625_, InteractionHand p_458563_) {
      return !this.isTamed() ? InteractionResult.PASS : super.mobInteract(p_460625_, p_458563_);
   }

   @Override
   public boolean canUseSlot(EquipmentSlot p_450363_) {
      return true;
   }
}
