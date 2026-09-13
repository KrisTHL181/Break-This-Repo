package net.minecraft.world.entity.monster.hoglin;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Hoglin extends Animal implements Enemy, HoglinBase {
   private static final EntityDataAccessor<Boolean> DATA_IMMUNE_TO_ZOMBIFICATION = SynchedEntityData.defineId(Hoglin.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.75F, 0.85F)
      .withEyeHeight(0.625F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.875F, 0.0F));
   private static final int MAX_HEALTH = 40;
   private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.3F;
   private static final int ATTACK_KNOCKBACK = 1;
   private static final float KNOCKBACK_RESISTANCE = 0.6F;
   private static final int ATTACK_DAMAGE = 6;
   private static final float BABY_ATTACK_DAMAGE = 0.5F;
   private static final boolean DEFAULT_IMMUNE_TO_ZOMBIFICATION = false;
   private static final int DEFAULT_TIME_IN_OVERWORLD = 0;
   private static final boolean DEFAULT_CANNOT_BE_HUNTED = false;
   public static final int CONVERSION_TIME = 300;
   private int attackAnimationRemainingTicks;
   private int timeInOverworld = 0;
   private boolean cannotBeHunted = false;
   private static final Brain.Provider<Hoglin> BRAIN_PROVIDER = Brain.provider(
      List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ADULT, SensorType.HOGLIN_SPECIFIC_SENSOR),
      var0 -> HoglinAi.getActivities()
   );

   public Hoglin(final EntityType<? extends Hoglin> type, final Level level) {
      super(type, level);
      this.xpReward = 5;
   }

   @VisibleForTesting
   public void setTimeInOverworld(final int timeInOverworld) {
      this.timeInOverworld = timeInOverworld;
   }

   @Override
   public boolean canBeLeashed() {
      return true;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 40.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3F)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.6F)
         .add(Attributes.ATTACK_KNOCKBACK, 1.0)
         .add(Attributes.ATTACK_DAMAGE, 6.0);
   }

   @Override
   public boolean doHurtTarget(final ServerLevel level, final Entity target) {
      if (target instanceof LivingEntity livingEntity) {
         this.attackAnimationRemainingTicks = 10;
         this.level().broadcastEntityEvent(this, (byte)4);
         this.makeSound(SoundEvents.HOGLIN_ATTACK);
         HoglinAi.onHitTarget(this, livingEntity);
         return HoglinBase.hurtAndThrowTarget(level, this, livingEntity);
      } else {
         return false;
      }
   }

   @Override
   protected void blockedByItem(final LivingEntity defender, final DamageSource source, final float damage) {
      if (this.isAdult()) {
         HoglinBase.throwTarget(this, defender);
      }
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      boolean wasHurt = super.hurtServer(level, source, damage);
      if (wasHurt && source.getEntity() instanceof LivingEntity sourceEntity) {
         HoglinAi.wasHurtBy(level, this, sourceEntity);
      }

      return wasHurt;
   }

   @Override
   protected Brain<Hoglin> makeBrain(final Brain.Packed packedBrain) {
      return BRAIN_PROVIDER.makeBrain(this, packedBrain);
   }

   @Override
   public Brain<Hoglin> getBrain() {
      return super.getBrain();
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("hoglinBrain");
      this.getBrain().tick(level, this);
      profiler.pop();
      HoglinAi.updateActivity(this);
      if (this.isConverting()) {
         this.timeInOverworld++;
         if (this.timeInOverworld > 300) {
            this.makeSound(SoundEvents.HOGLIN_CONVERTED_TO_ZOMBIFIED);
            this.finishConversion();
         }
      } else {
         this.timeInOverworld = 0;
      }
   }

   @Override
   public void aiStep() {
      if (this.attackAnimationRemainingTicks > 0) {
         this.attackAnimationRemainingTicks--;
      }

      super.aiStep();
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   @Override
   protected void ageBoundaryReached() {
      if (this.isBaby()) {
         this.xpReward = 3;
         this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.5);
      } else {
         this.xpReward = 5;
         this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6.0);
      }
   }

   public static boolean checkHoglinSpawnRules(
      final EntityType<Hoglin> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return !level.getBlockState(pos.below()).is(Blocks.NETHER_WART_BLOCK);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      if (level.getRandom().nextFloat() < 0.2F) {
         this.setBaby(true);
      }

      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return true;
   }

   @Override
   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      if (HoglinAi.isPosNearNearestRepellent(this, pos)) {
         return -1.0F;
      } else {
         return level.getBlockState(pos.below()).is(Blocks.CRIMSON_NYLIUM) ? 10.0F : 0.0F;
      }
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      InteractionResult interactionSucceeded = super.mobInteract(player, hand);
      if (interactionSucceeded.consumesAction()) {
         this.setPersistenceRequired();
      }

      return interactionSucceeded;
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 4) {
         this.attackAnimationRemainingTicks = 10;
         this.makeSound(SoundEvents.HOGLIN_ATTACK);
      } else {
         super.handleEntityEvent(id);
      }
   }

   @Override
   public int getAttackAnimationRemainingTicks() {
      return this.attackAnimationRemainingTicks;
   }

   @Override
   public boolean shouldDropExperience() {
      return true;
   }

   @Override
   protected int getBaseExperienceReward(final ServerLevel level) {
      return this.xpReward;
   }

   private void finishConversion() {
      this.convertTo(
         EntityTypes.ZOGLIN, ConversionParams.single(this, true, false), zoglin -> zoglin.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0))
      );
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.HOGLIN_FOOD);
   }

   public boolean isAdult() {
      return !this.isBaby();
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_IMMUNE_TO_ZOMBIFICATION, false);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("IsImmuneToZombification", this.isImmuneToZombification());
      output.putInt("TimeInOverworld", this.timeInOverworld);
      output.putBoolean("CannotBeHunted", this.cannotBeHunted);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setImmuneToZombification(input.getBooleanOr("IsImmuneToZombification", false));
      this.timeInOverworld = input.getIntOr("TimeInOverworld", 0);
      this.setCannotBeHunted(input.getBooleanOr("CannotBeHunted", false));
   }

   public void setImmuneToZombification(final boolean isImmuneToZombification) {
      this.getEntityData().set(DATA_IMMUNE_TO_ZOMBIFICATION, isImmuneToZombification);
   }

   private boolean isImmuneToZombification() {
      return this.getEntityData().get(DATA_IMMUNE_TO_ZOMBIFICATION);
   }

   public boolean isConverting() {
      return !this.isImmuneToZombification()
         && !this.isNoAi()
         && this.level().environmentAttributes().getValue(EnvironmentAttributes.PIGLINS_ZOMBIFY, this.position());
   }

   private void setCannotBeHunted(final boolean cannotBeHunted) {
      this.cannotBeHunted = cannotBeHunted;
   }

   public boolean canBeHunted() {
      return this.isAdult() && !this.cannotBeHunted;
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      Hoglin offspring = EntityTypes.HOGLIN.create(level, EntitySpawnReason.BREEDING);
      if (offspring != null) {
         offspring.setPersistenceRequired();
      }

      return offspring;
   }

   @Override
   public boolean canFallInLove() {
      return !HoglinAi.isPacified(this) && super.canFallInLove();
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.level().isClientSide() ? null : HoglinAi.getSoundForCurrentActivity(this).orElse(null);
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.HOGLIN_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.HOGLIN_DEATH;
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.HOSTILE_SWIM;
   }

   @Override
   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.HOSTILE_SPLASH;
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(SoundEvents.HOGLIN_STEP, 0.15F, 1.0F);
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }
}
