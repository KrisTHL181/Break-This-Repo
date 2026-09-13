package net.minecraft.world.entity.animal.polarbear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class PolarBear extends Animal implements NeutralMob {
   private static final EntityDataAccessor<Boolean> DATA_STANDING_ID = SynchedEntityData.defineId(PolarBear.class, EntityDataSerializers.BOOLEAN);
   private static final float STAND_ANIMATION_TICKS = 6.0F;
   private float clientSideStandAnimationO;
   private float clientSideStandAnimation;
   private int warningSoundTicks;
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   private long persistentAngerEndTime;
   private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

   public PolarBear(EntityType<? extends PolarBear> p_460088_, Level p_458919_) {
      super(p_460088_, p_458919_);
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_456683_, AgeableMob p_454291_) {
      return EntityType.POLAR_BEAR.create(p_456683_, EntitySpawnReason.BREEDING);
   }

   @Override
   public boolean isFood(ItemStack p_451357_) {
      return false;
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new PolarBear.PolarBearMeleeAttackGoal());
      this.goalSelector
         .addGoal(1, new PanicGoal(this, 2.0, p_454228_ -> p_454228_.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
      this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
      this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new PolarBear.PolarBearHurtByTargetGoal());
      this.targetSelector.addGoal(2, new PolarBear.PolarBearAttackPlayersGoal());
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Fox.class, 10, true, true, null));
      this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.FOLLOW_RANGE, 20.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.ATTACK_DAMAGE, 6.0);
   }

   public static boolean checkPolarBearSpawnRules(
      EntityType<PolarBear> p_459130_, LevelAccessor p_454859_, EntitySpawnReason p_456846_, BlockPos p_456836_, RandomSource p_460547_
   ) {
      Holder<Biome> holder = p_454859_.getBiome(p_456836_);
      return !holder.is(BiomeTags.POLAR_BEARS_SPAWN_ON_ALTERNATE_BLOCKS)
         ? checkAnimalSpawnRules(p_459130_, p_454859_, p_456846_, p_456836_, p_460547_)
         : isBrightEnoughToSpawn(p_454859_, p_456836_) && p_454859_.getBlockState(p_456836_.below()).is(BlockTags.POLAR_BEARS_SPAWNABLE_ON_ALTERNATE);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_452343_) {
      super.readAdditionalSaveData(p_452343_);
      this.readPersistentAngerSaveData(this.level(), p_452343_);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_459631_) {
      super.addAdditionalSaveData(p_459631_);
      this.addPersistentAngerSaveData(p_459631_);
   }

   @Override
   public void startPersistentAngerTimer() {
      this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   @Override
   public void setPersistentAngerEndTime(long p_450441_) {
      this.persistentAngerEndTime = p_450441_;
   }

   @Override
   public long getPersistentAngerEndTime() {
      return this.persistentAngerEndTime;
   }

   @Override
   public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> p_452912_) {
      this.persistentAngerTarget = p_452912_;
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isBaby() ? SoundEvents.POLAR_BEAR_AMBIENT_BABY : SoundEvents.POLAR_BEAR_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_452944_) {
      return SoundEvents.POLAR_BEAR_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.POLAR_BEAR_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_451348_, BlockState p_458577_) {
      this.playSound(SoundEvents.POLAR_BEAR_STEP, 0.15F, 1.0F);
   }

   protected void playWarningSound() {
      if (this.warningSoundTicks <= 0) {
         this.makeSound(SoundEvents.POLAR_BEAR_WARNING);
         this.warningSoundTicks = 40;
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_455170_) {
      super.defineSynchedData(p_455170_);
      p_455170_.define(DATA_STANDING_ID, false);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         if (this.clientSideStandAnimation != this.clientSideStandAnimationO) {
            this.refreshDimensions();
         }

         this.clientSideStandAnimationO = this.clientSideStandAnimation;
         if (this.isStanding()) {
            this.clientSideStandAnimation = Mth.clamp(this.clientSideStandAnimation + 1.0F, 0.0F, 6.0F);
         } else {
            this.clientSideStandAnimation = Mth.clamp(this.clientSideStandAnimation - 1.0F, 0.0F, 6.0F);
         }
      }

      if (this.warningSoundTicks > 0) {
         this.warningSoundTicks--;
      }

      if (!this.level().isClientSide()) {
         this.updatePersistentAnger((ServerLevel)this.level(), true);
      }
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_451292_) {
      if (this.clientSideStandAnimation > 0.0F) {
         float f = this.clientSideStandAnimation / 6.0F;
         float f1 = 1.0F + f;
         return super.getDefaultDimensions(p_451292_).scale(1.0F, f1);
      } else {
         return super.getDefaultDimensions(p_451292_);
      }
   }

   public boolean isStanding() {
      return this.entityData.get(DATA_STANDING_ID);
   }

   public void setStanding(boolean p_454411_) {
      this.entityData.set(DATA_STANDING_ID, p_454411_);
   }

   public float getStandingAnimationScale(float p_457661_) {
      return Mth.lerp(p_457661_, this.clientSideStandAnimationO, this.clientSideStandAnimation) / 6.0F;
   }

   @Override
   protected float getWaterSlowDown() {
      return 0.98F;
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_453975_, DifficultyInstance p_453574_, EntitySpawnReason p_457280_, @Nullable SpawnGroupData p_451947_
   ) {
      if (p_451947_ == null) {
         p_451947_ = new AgeableMob.AgeableMobGroupData(1.0F);
      }

      return super.finalizeSpawn(p_453975_, p_453574_, p_457280_, p_451947_);
   }

   class PolarBearAttackPlayersGoal extends NearestAttackableTargetGoal<Player> {
      public PolarBearAttackPlayersGoal() {
         super(PolarBear.this, Player.class, 20, true, true, null);
      }

      @Override
      public boolean canUse() {
         if (PolarBear.this.isBaby()) {
            return false;
         }

         if (super.canUse()) {
            for (PolarBear polarbear : PolarBear.this.level().getEntitiesOfClass(PolarBear.class, PolarBear.this.getBoundingBox().inflate(8.0, 4.0, 8.0))) {
               if (polarbear.isBaby()) {
                  return true;
               }
            }
         }

         return false;
      }

      @Override
      protected double getFollowDistance() {
         return super.getFollowDistance() * 0.5;
      }
   }

   class PolarBearHurtByTargetGoal extends HurtByTargetGoal {
      public PolarBearHurtByTargetGoal() {
         super(PolarBear.this);
      }

      @Override
      public void start() {
         super.start();
         if (PolarBear.this.isBaby()) {
            this.alertOthers();
            this.stop();
         }
      }

      @Override
      protected void alertOther(Mob p_456033_, LivingEntity p_450188_) {
         if (p_456033_ instanceof PolarBear && !p_456033_.isBaby()) {
            super.alertOther(p_456033_, p_450188_);
         }
      }
   }

   class PolarBearMeleeAttackGoal extends MeleeAttackGoal {
      public PolarBearMeleeAttackGoal() {
         super(PolarBear.this, 1.25, true);
      }

      @Override
      protected void checkAndPerformAttack(LivingEntity p_450628_) {
         if (this.canPerformAttack(p_450628_)) {
            this.resetAttackCooldown();
            this.mob.doHurtTarget(getServerLevel(this.mob), p_450628_);
            PolarBear.this.setStanding(false);
         } else if (this.mob.distanceToSqr(p_450628_) < (p_450628_.getBbWidth() + 3.0F) * (p_450628_.getBbWidth() + 3.0F)) {
            if (this.isTimeToAttack()) {
               PolarBear.this.setStanding(false);
               this.resetAttackCooldown();
            }

            if (this.getTicksUntilNextAttack() <= 10) {
               PolarBear.this.setStanding(true);
               PolarBear.this.playWarningSound();
            }
         } else {
            this.resetAttackCooldown();
            PolarBear.this.setStanding(false);
         }
      }

      @Override
      public void stop() {
         PolarBear.this.setStanding(false);
         super.stop();
      }
   }
}
