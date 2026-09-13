package net.minecraft.world.entity.monster;

import com.google.common.annotations.VisibleForTesting;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.ConversionType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public class Slime extends Mob implements Enemy {
   private static final EntityDataAccessor<Integer> ID_SIZE = SynchedEntityData.defineId(Slime.class, EntityDataSerializers.INT);
   public static final int MIN_SIZE = 1;
   public static final int MAX_SIZE = 127;
   public static final int MAX_NATURAL_SIZE = 4;
   private static final boolean DEFAULT_WAS_ON_GROUND = false;
   public float targetSquish;
   public float squish;
   public float oSquish;
   private boolean wasOnGround = false;

   public Slime(EntityType<? extends Slime> p_33588_, Level p_33589_) {
      super(p_33588_, p_33589_);
      this.fixupDimensions();
      this.moveControl = new Slime.SlimeMoveControl(this);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new Slime.SlimeFloatGoal(this));
      this.goalSelector.addGoal(2, new Slime.SlimeAttackGoal(this));
      this.goalSelector.addGoal(3, new Slime.SlimeRandomDirectionGoal(this));
      this.goalSelector.addGoal(5, new Slime.SlimeKeepOnJumpingGoal(this));
      this.targetSelector
         .addGoal(
            1,
            new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (p_449691_, p_449692_) -> Math.abs(p_449691_.getY() - this.getY()) <= 4.0)
         );
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_335838_) {
      super.defineSynchedData(p_335838_);
      p_335838_.define(ID_SIZE, 1);
   }

   @VisibleForTesting
   public void setSize(int p_33594_, boolean p_33595_) {
      int i = Mth.clamp(p_33594_, 1, 127);
      this.entityData.set(ID_SIZE, i);
      this.reapplyPosition();
      this.refreshDimensions();
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(i * i);
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2F + 0.1F * i);
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(i);
      if (p_33595_) {
         this.setHealth(this.getMaxHealth());
      }

      this.xpReward = i;
   }

   public int getSize() {
      return this.entityData.get(ID_SIZE);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_407585_) {
      super.addAdditionalSaveData(p_407585_);
      p_407585_.putInt("Size", this.getSize() - 1);
      p_407585_.putBoolean("wasOnGround", this.wasOnGround);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_410374_) {
      this.setSize(p_410374_.getIntOr("Size", 0) + 1, false);
      super.readAdditionalSaveData(p_410374_);
      this.wasOnGround = p_410374_.getBooleanOr("wasOnGround", false);
   }

   public boolean isTiny() {
      return this.getSize() <= 1;
   }

   protected ParticleOptions getParticleType() {
      return ParticleTypes.ITEM_SLIME;
   }

   @Override
   public void tick() {
      this.oSquish = this.squish;
      this.squish = this.squish + (this.targetSquish - this.squish) * 0.5F;
      super.tick();
      if (this.onGround() && !this.wasOnGround) {
         float f = this.getDimensions(this.getPose()).width() * 2.0F;
         float f1 = f / 2.0F;

         for (int i = 0; i < f * 16.0F; i++) {
            float f2 = this.random.nextFloat() * (float) (Math.PI * 2);
            float f3 = this.random.nextFloat() * 0.5F + 0.5F;
            float f4 = Mth.sin(f2) * f1 * f3;
            float f5 = Mth.cos(f2) * f1 * f3;
            this.level().addParticle(this.getParticleType(), this.getX() + f4, this.getY(), this.getZ() + f5, 0.0, 0.0, 0.0);
         }

         this.playSound(this.getSquishSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
         this.targetSquish = -0.5F;
      } else if (!this.onGround() && this.wasOnGround) {
         this.targetSquish = 1.0F;
      }

      this.wasOnGround = this.onGround();
      this.decreaseSquish();
   }

   protected void decreaseSquish() {
      this.targetSquish *= 0.6F;
   }

   protected int getJumpDelay() {
      return this.random.nextInt(20) + 10;
   }

   @Override
   public void refreshDimensions() {
      double d0 = this.getX();
      double d1 = this.getY();
      double d2 = this.getZ();
      super.refreshDimensions();
      this.setPos(d0, d1, d2);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_33609_) {
      if (ID_SIZE.equals(p_33609_)) {
         this.refreshDimensions();
         this.setYRot(this.yHeadRot);
         this.yBodyRot = this.yHeadRot;
         if (this.isInWater() && this.random.nextInt(20) == 0) {
            this.doWaterSplashEffect();
         }
      }

      super.onSyncedDataUpdated(p_33609_);
   }

   @Override
   public EntityType<? extends Slime> getType() {
      return (EntityType<? extends Slime>)super.getType();
   }

   @Override
   public void remove(Entity.RemovalReason p_149847_) {
      int i = this.getSize();
      if (!this.level().isClientSide() && i > 1 && this.isDeadOrDying()) {
         float f = this.getDimensions(this.getPose()).width();
         float f1 = f / 2.0F;
         int j = i / 2;
         int k = 2 + this.random.nextInt(3);
         PlayerTeam playerteam = this.getTeam();

         for (int l = 0; l < k; l++) {
            float f2 = (l % 2 - 0.5F) * f1;
            float f3 = (l / 2 - 0.5F) * f1;
            this.convertTo(
               this.getType(), new ConversionParams(ConversionType.SPLIT_ON_DEATH, false, false, playerteam), EntitySpawnReason.TRIGGERED, p_449696_ -> {
                  p_449696_.setSize(j, true);
                  p_449696_.snapTo(this.getX() + f2, this.getY() + 0.5, this.getZ() + f3, this.random.nextFloat() * 360.0F, 0.0F);
               }
            );
         }
      }

      super.remove(p_149847_);
   }

   @Override
   public void push(Entity p_33636_) {
      super.push(p_33636_);
      if (p_33636_ instanceof IronGolem && this.isDealsDamage()) {
         this.dealDamage((LivingEntity)p_33636_);
      }
   }

   @Override
   public void playerTouch(Player p_33611_) {
      if (this.isDealsDamage()) {
         this.dealDamage(p_33611_);
      }
   }

   protected void dealDamage(LivingEntity p_33638_) {
      if (this.level() instanceof ServerLevel serverlevel && this.isAlive() && this.isWithinMeleeAttackRange(p_33638_) && this.hasLineOfSight(p_33638_)) {
         DamageSource damagesource = this.damageSources().mobAttack(this);
         if (p_33638_.hurtServer(serverlevel, damagesource, this.getAttackDamage())) {
            this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            EnchantmentHelper.doPostAttackEffects(serverlevel, p_33638_, damagesource);
         }
      }
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity p_298024_, EntityDimensions p_298393_, float p_298662_) {
      return new Vec3(0.0, p_298393_.height() - 0.015625 * this.getSize() * p_298662_, 0.0);
   }

   protected boolean isDealsDamage() {
      return !this.isTiny() && this.isEffectiveAi();
   }

   protected float getAttackDamage() {
      return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_33631_) {
      return this.isTiny() ? SoundEvents.SLIME_HURT_SMALL : SoundEvents.SLIME_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.isTiny() ? SoundEvents.SLIME_DEATH_SMALL : SoundEvents.SLIME_DEATH;
   }

   protected SoundEvent getSquishSound() {
      return this.isTiny() ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.SLIME_SQUISH;
   }

   public static boolean checkSlimeSpawnRules(
      EntityType<Slime> p_219113_, LevelAccessor p_219114_, EntitySpawnReason p_369328_, BlockPos p_219116_, RandomSource p_219117_
   ) {
      if (p_219114_.getDifficulty() != Difficulty.PEACEFUL) {
         if (EntitySpawnReason.isSpawner(p_369328_)) {
            return checkMobSpawnRules(p_219113_, p_219114_, p_369328_, p_219116_, p_219117_);
         }

         if (p_219114_.getBiome(p_219116_).is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS) && p_219116_.getY() > 50 && p_219116_.getY() < 70) {
            float f = p_219114_.environmentAttributes().getValue(EnvironmentAttributes.SURFACE_SLIME_SPAWN_CHANCE, p_219116_);
            if (p_219117_.nextFloat() < f && p_219114_.getMaxLocalRawBrightness(p_219116_) <= p_219117_.nextInt(8)) {
               return checkMobSpawnRules(p_219113_, p_219114_, p_369328_, p_219116_, p_219117_);
            }
         }

         if (!(p_219114_ instanceof WorldGenLevel)) {
            return false;
         }

         ChunkPos chunkpos = new ChunkPos(p_219116_);
         boolean flag = WorldgenRandom.seedSlimeChunk(chunkpos.x, chunkpos.z, ((WorldGenLevel)p_219114_).getSeed(), 987234911L).nextInt(10) == 0;
         if (p_219117_.nextInt(10) == 0 && flag && p_219116_.getY() < 40) {
            return checkMobSpawnRules(p_219113_, p_219114_, p_369328_, p_219116_, p_219117_);
         }
      }

      return false;
   }

   @Override
   protected float getSoundVolume() {
      return 0.4F * this.getSize();
   }

   @Override
   public int getMaxHeadXRot() {
      return 0;
   }

   protected boolean doPlayJumpSound() {
      return this.getSize() > 0;
   }

   @Override
   public void jumpFromGround() {
      Vec3 vec3 = this.getDeltaMovement();
      this.setDeltaMovement(vec3.x, this.getJumpPower(), vec3.z);
      this.needsSync = true;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_33601_, DifficultyInstance p_33602_, EntitySpawnReason p_364449_, @Nullable SpawnGroupData p_33604_
   ) {
      RandomSource randomsource = p_33601_.getRandom();
      int i = randomsource.nextInt(3);
      if (i < 2 && randomsource.nextFloat() < 0.5F * p_33602_.getSpecialMultiplier()) {
         i++;
      }

      int j = 1 << i;
      this.setSize(j, true);
      return super.finalizeSpawn(p_33601_, p_33602_, p_364449_, p_33604_);
   }

   float getSoundPitch() {
      float f = this.isTiny() ? 1.4F : 0.8F;
      return ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * f;
   }

   protected SoundEvent getJumpSound() {
      return this.isTiny() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_336379_) {
      return super.getDefaultDimensions(p_336379_).scale(this.getSize());
   }

   static class SlimeAttackGoal extends Goal {
      private final Slime slime;
      private int growTiredTimer;

      public SlimeAttackGoal(Slime p_33648_) {
         this.slime = p_33648_;
         this.setFlags(EnumSet.of(Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         LivingEntity livingentity = this.slime.getTarget();
         if (livingentity == null) {
            return false;
         } else {
            return !this.slime.canAttack(livingentity) ? false : this.slime.getMoveControl() instanceof Slime.SlimeMoveControl;
         }
      }

      @Override
      public void start() {
         this.growTiredTimer = reducedTickDelay(300);
         super.start();
      }

      @Override
      public boolean canContinueToUse() {
         LivingEntity livingentity = this.slime.getTarget();
         if (livingentity == null) {
            return false;
         } else {
            return !this.slime.canAttack(livingentity) ? false : --this.growTiredTimer > 0;
         }
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         LivingEntity livingentity = this.slime.getTarget();
         if (livingentity != null) {
            this.slime.lookAt(livingentity, 10.0F, 10.0F);
         }

         if (this.slime.getMoveControl() instanceof Slime.SlimeMoveControl slime$slimemovecontrol) {
            slime$slimemovecontrol.setDirection(this.slime.getYRot(), this.slime.isDealsDamage());
         }
      }
   }

   static class SlimeFloatGoal extends Goal {
      private final Slime slime;

      public SlimeFloatGoal(Slime p_33655_) {
         this.slime = p_33655_;
         this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
         p_33655_.getNavigation().setCanFloat(true);
      }

      @Override
      public boolean canUse() {
         return (this.slime.isInWater() || this.slime.isInLava()) && this.slime.getMoveControl() instanceof Slime.SlimeMoveControl;
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         if (this.slime.getRandom().nextFloat() < 0.8F) {
            this.slime.getJumpControl().jump();
         }

         if (this.slime.getMoveControl() instanceof Slime.SlimeMoveControl slime$slimemovecontrol) {
            slime$slimemovecontrol.setWantedMovement(1.2);
         }
      }
   }

   static class SlimeKeepOnJumpingGoal extends Goal {
      private final Slime slime;

      public SlimeKeepOnJumpingGoal(Slime p_33660_) {
         this.slime = p_33660_;
         this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         return !this.slime.isPassenger();
      }

      @Override
      public void tick() {
         if (this.slime.getMoveControl() instanceof Slime.SlimeMoveControl slime$slimemovecontrol) {
            slime$slimemovecontrol.setWantedMovement(1.0);
         }
      }
   }

   static class SlimeMoveControl extends MoveControl {
      private float yRot;
      private int jumpDelay;
      private final Slime slime;
      private boolean isAggressive;

      public SlimeMoveControl(Slime p_33668_) {
         super(p_33668_);
         this.slime = p_33668_;
         this.yRot = 180.0F * p_33668_.getYRot() / (float) Math.PI;
      }

      public void setDirection(float p_33673_, boolean p_33674_) {
         this.yRot = p_33673_;
         this.isAggressive = p_33674_;
      }

      public void setWantedMovement(double p_33671_) {
         this.speedModifier = p_33671_;
         this.operation = MoveControl.Operation.MOVE_TO;
      }

      @Override
      public void tick() {
         this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
         this.mob.yHeadRot = this.mob.getYRot();
         this.mob.yBodyRot = this.mob.getYRot();
         if (this.operation != MoveControl.Operation.MOVE_TO) {
            this.mob.setZza(0.0F);
         } else {
            this.operation = MoveControl.Operation.WAIT;
            if (this.mob.onGround()) {
               this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
               if (this.jumpDelay-- <= 0) {
                  this.jumpDelay = this.slime.getJumpDelay();
                  if (this.isAggressive) {
                     this.jumpDelay /= 3;
                  }

                  this.slime.getJumpControl().jump();
                  if (this.slime.doPlayJumpSound()) {
                     this.slime.playSound(this.slime.getJumpSound(), this.slime.getSoundVolume(), this.slime.getSoundPitch());
                  }
               } else {
                  this.slime.xxa = 0.0F;
                  this.slime.zza = 0.0F;
                  this.mob.setSpeed(0.0F);
               }
            } else {
               this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            }
         }
      }
   }

   static class SlimeRandomDirectionGoal extends Goal {
      private final Slime slime;
      private float chosenDegrees;
      private int nextRandomizeTime;

      public SlimeRandomDirectionGoal(Slime p_33679_) {
         this.slime = p_33679_;
         this.setFlags(EnumSet.of(Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         return this.slime.getTarget() == null
            && (this.slime.onGround() || this.slime.isInWater() || this.slime.isInLava() || this.slime.hasEffect(MobEffects.LEVITATION))
            && this.slime.getMoveControl() instanceof Slime.SlimeMoveControl;
      }

      @Override
      public void tick() {
         if (--this.nextRandomizeTime <= 0) {
            this.nextRandomizeTime = this.adjustedTickDelay(40 + this.slime.getRandom().nextInt(60));
            this.chosenDegrees = this.slime.getRandom().nextInt(360);
         }

         if (this.slime.getMoveControl() instanceof Slime.SlimeMoveControl slime$slimemovecontrol) {
            slime$slimemovecontrol.setDirection(this.chosenDegrees, false);
         }
      }
   }
}
