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
import net.minecraft.world.entity.EntityTypes;
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
   private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.SKELETON_HORSE
      .getDimensions()
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityTypes.SKELETON_HORSE.getHeight() - 0.25F, 0.0F))
      .scale(0.7F);
   private boolean isTrap = false;
   private int trapTime = 0;

   public SkeletonHorse(final EntityType<? extends SkeletonHorse> type, final Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 15.0).add(Attributes.MOVEMENT_SPEED, 0.2F);
   }

   public static boolean checkSkeletonHorseSpawnRules(
      final EntityType<? extends Animal> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return !EntitySpawnReason.isSpawner(spawnReason)
         ? Animal.checkAnimalSpawnRules(type, level, spawnReason, pos, random)
         : EntitySpawnReason.ignoresLightRequirements(spawnReason) || isBrightEnoughToSpawn(level, pos);
   }

   @Override
   protected void randomizeAttributes(final RandomSource random) {
      this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(random::nextDouble));
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
   protected SoundEvent getHurtSound(final DamageSource source) {
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
   protected void playSwimSound(final float volume) {
      if (this.onGround()) {
         super.playSwimSound(0.3F);
      } else {
         super.playSwimSound(Math.min(0.1F, volume * 25.0F));
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
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.isPersistenceRequired() && this.isTrap() && this.trapTime++ >= 18000) {
         this.discard();
      }
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("SkeletonTrap", this.isTrap());
      output.putInt("SkeletonTrapTime", this.trapTime);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setTrap(input.getBooleanOr("SkeletonTrap", false));
      this.trapTime = input.getIntOr("SkeletonTrapTime", 0);
   }

   @Override
   protected float getWaterSlowDown() {
      return 0.96F;
   }

   public boolean isTrap() {
      return this.isTrap;
   }

   public void setTrap(final boolean trap) {
      if (trap != this.isTrap) {
         this.isTrap = trap;
         if (trap) {
            this.goalSelector.addGoal(1, this.skeletonTrapGoal);
         } else {
            this.goalSelector.removeGoal(this.skeletonTrapGoal);
         }
      }
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return EntityTypes.SKELETON_HORSE.create(level, EntitySpawnReason.BREEDING);
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      return !this.isTamed() ? InteractionResult.PASS : super.mobInteract(player, hand);
   }

   @Override
   public boolean canUseSlot(final EquipmentSlot slot) {
      return true;
   }

   @Override
   public boolean canAgeUp() {
      return false;
   }
}
