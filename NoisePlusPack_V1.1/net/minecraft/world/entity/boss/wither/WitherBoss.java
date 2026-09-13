package net.minecraft.world.entity.boss.wither;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class WitherBoss extends Monster implements RangedAttackMob {
   private static final EntityDataAccessor<Integer> DATA_TARGET_A = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_TARGET_B = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_TARGET_C = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
   private static final List<EntityDataAccessor<Integer>> DATA_TARGETS = ImmutableList.of(DATA_TARGET_A, DATA_TARGET_B, DATA_TARGET_C);
   private static final EntityDataAccessor<Integer> DATA_ID_INV = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
   private static final int INVULNERABLE_TICKS = 220;
   private static final int DEFAULT_INVULNERABLE_TICKS = 0;
   private final float[] xRotHeads = new float[2];
   private final float[] yRotHeads = new float[2];
   private final float[] xRotOHeads = new float[2];
   private final float[] yRotOHeads = new float[2];
   private final int[] nextHeadUpdate = new int[2];
   private final int[] idleHeadUpdates = new int[2];
   private int destroyBlocksTick;
   private final ServerBossEvent bossEvent = (ServerBossEvent)new ServerBossEvent(
         this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS
      )
      .setDarkenScreen(true);
   private static final TargetingConditions.Selector LIVING_ENTITY_SELECTOR = (p_449676_, p_449677_) -> !p_449676_.getType().is(EntityTypeTags.WITHER_FRIENDS)
      && p_449676_.attackable();
   private static final TargetingConditions TARGETING_CONDITIONS = TargetingConditions.forCombat().range(20.0).selector(LIVING_ENTITY_SELECTOR);

   public WitherBoss(EntityType<? extends WitherBoss> p_31437_, Level p_31438_) {
      super(p_31437_, p_31438_);
      this.moveControl = new FlyingMoveControl(this, 10, false);
      this.setHealth(this.getMaxHealth());
      this.xpReward = 50;
   }

   @Override
   protected PathNavigation createNavigation(Level p_186262_) {
      FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, p_186262_);
      flyingpathnavigation.setCanOpenDoors(false);
      flyingpathnavigation.setCanFloat(true);
      return flyingpathnavigation;
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new WitherBoss.WitherDoNothingGoal());
      this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 20.0F));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, false, false, LIVING_ENTITY_SELECTOR));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_332370_) {
      super.defineSynchedData(p_332370_);
      p_332370_.define(DATA_TARGET_A, 0);
      p_332370_.define(DATA_TARGET_B, 0);
      p_332370_.define(DATA_TARGET_C, 0);
      p_332370_.define(DATA_ID_INV, 0);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_407664_) {
      super.addAdditionalSaveData(p_407664_);
      p_407664_.putInt("Invul", this.getInvulnerableTicks());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_410617_) {
      super.readAdditionalSaveData(p_410617_);
      this.setInvulnerableTicks(p_410617_.getIntOr("Invul", 0));
      if (this.hasCustomName()) {
         this.bossEvent.setName(this.getDisplayName());
      }
   }

   @Override
   public void setCustomName(@Nullable Component p_31476_) {
      super.setCustomName(p_31476_);
      this.bossEvent.setName(this.getDisplayName());
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.WITHER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_31500_) {
      return SoundEvents.WITHER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.WITHER_DEATH;
   }

   @Override
   public void aiStep() {
      Vec3 vec3 = this.getDeltaMovement().multiply(1.0, 0.6, 1.0);
      if (!this.level().isClientSide() && this.getAlternativeTarget(0) > 0) {
         Entity entity = this.level().getEntity(this.getAlternativeTarget(0));
         if (entity != null) {
            double d0 = vec3.y;
            if (this.getY() < entity.getY() || !this.isPowered() && this.getY() < entity.getY() + 5.0) {
               d0 = Math.max(0.0, d0);
               d0 += 0.3 - d0 * 0.6F;
            }

            vec3 = new Vec3(vec3.x, d0, vec3.z);
            Vec3 vec31 = new Vec3(entity.getX() - this.getX(), 0.0, entity.getZ() - this.getZ());
            if (vec31.horizontalDistanceSqr() > 9.0) {
               Vec3 vec32 = vec31.normalize();
               vec3 = vec3.add(vec32.x * 0.3 - vec3.x * 0.6, 0.0, vec32.z * 0.3 - vec3.z * 0.6);
            }
         }
      }

      this.setDeltaMovement(vec3);
      if (vec3.horizontalDistanceSqr() > 0.05) {
         this.setYRot((float)Mth.atan2(vec3.z, vec3.x) * (180.0F / (float)Math.PI) - 90.0F);
      }

      super.aiStep();

      for (int i = 0; i < 2; i++) {
         this.yRotOHeads[i] = this.yRotHeads[i];
         this.xRotOHeads[i] = this.xRotHeads[i];
      }

      for (int j = 0; j < 2; j++) {
         int k = this.getAlternativeTarget(j + 1);
         Entity entity1 = null;
         if (k > 0) {
            entity1 = this.level().getEntity(k);
         }

         if (entity1 != null) {
            double d9 = this.getHeadX(j + 1);
            double d1 = this.getHeadY(j + 1);
            double d3 = this.getHeadZ(j + 1);
            double d4 = entity1.getX() - d9;
            double d5 = entity1.getEyeY() - d1;
            double d6 = entity1.getZ() - d3;
            double d7 = Math.sqrt(d4 * d4 + d6 * d6);
            float f1 = (float)(Mth.atan2(d6, d4) * 180.0 / (float) Math.PI) - 90.0F;
            float f2 = (float)(-(Mth.atan2(d5, d7) * 180.0 / (float) Math.PI));
            this.xRotHeads[j] = this.rotlerp(this.xRotHeads[j], f2, 40.0F);
            this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], f1, 10.0F);
         } else {
            this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], this.yBodyRot, 10.0F);
         }
      }

      boolean flag = this.isPowered();

      for (int l = 0; l < 3; l++) {
         double d8 = this.getHeadX(l);
         double d10 = this.getHeadY(l);
         double d2 = this.getHeadZ(l);
         float f = 0.3F * this.getScale();
         this.level()
            .addParticle(
               ParticleTypes.SMOKE,
               d8 + this.random.nextGaussian() * f,
               d10 + this.random.nextGaussian() * f,
               d2 + this.random.nextGaussian() * f,
               0.0,
               0.0,
               0.0
            );
         if (flag && this.level().random.nextInt(4) == 0) {
            this.level()
               .addParticle(
                  ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.7F, 0.7F, 0.5F),
                  d8 + this.random.nextGaussian() * f,
                  d10 + this.random.nextGaussian() * f,
                  d2 + this.random.nextGaussian() * f,
                  0.0,
                  0.0,
                  0.0
               );
         }
      }

      if (this.getInvulnerableTicks() > 0) {
         float f3 = 3.3F * this.getScale();

         for (int i1 = 0; i1 < 3; i1++) {
            this.level()
               .addParticle(
                  ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.7F, 0.7F, 0.9F),
                  this.getX() + this.random.nextGaussian(),
                  this.getY() + this.random.nextFloat() * f3,
                  this.getZ() + this.random.nextGaussian(),
                  0.0,
                  0.0,
                  0.0
               );
         }
      }
   }

   @Override
   protected void customServerAiStep(ServerLevel p_363567_) {
      if (this.getInvulnerableTicks() > 0) {
         int j = this.getInvulnerableTicks() - 1;
         this.bossEvent.setProgress(1.0F - j / 220.0F);
         if (j <= 0) {
            p_363567_.explode(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, Level.ExplosionInteraction.MOB);
            if (!this.isSilent()) {
               p_363567_.globalLevelEvent(1023, this.blockPosition(), 0);
            }
         }

         this.setInvulnerableTicks(j);
         if (this.tickCount % 10 == 0) {
            this.heal(10.0F);
         }
      } else {
         super.customServerAiStep(p_363567_);

         for (int i = 1; i < 3; i++) {
            if (this.tickCount >= this.nextHeadUpdate[i - 1]) {
               this.nextHeadUpdate[i - 1] = this.tickCount + 10 + this.random.nextInt(10);
               if ((p_363567_.getDifficulty() == Difficulty.NORMAL || p_363567_.getDifficulty() == Difficulty.HARD) && this.idleHeadUpdates[i - 1]++ > 15) {
                  float f = 10.0F;
                  float f1 = 5.0F;
                  double d0 = Mth.nextDouble(this.random, this.getX() - 10.0, this.getX() + 10.0);
                  double d1 = Mth.nextDouble(this.random, this.getY() - 5.0, this.getY() + 5.0);
                  double d2 = Mth.nextDouble(this.random, this.getZ() - 10.0, this.getZ() + 10.0);
                  this.performRangedAttack(i + 1, d0, d1, d2, true);
                  this.idleHeadUpdates[i - 1] = 0;
               }

               int k = this.getAlternativeTarget(i);
               if (k > 0) {
                  LivingEntity livingentity = (LivingEntity)p_363567_.getEntity(k);
                  if (livingentity != null && this.canAttack(livingentity) && !(this.distanceToSqr(livingentity) > 900.0) && this.hasLineOfSight(livingentity)) {
                     this.performRangedAttack(i + 1, livingentity);
                     this.nextHeadUpdate[i - 1] = this.tickCount + 40 + this.random.nextInt(20);
                     this.idleHeadUpdates[i - 1] = 0;
                  } else {
                     this.setAlternativeTarget(i, 0);
                  }
               } else {
                  List<LivingEntity> list = p_363567_.getNearbyEntities(
                     LivingEntity.class, TARGETING_CONDITIONS, this, this.getBoundingBox().inflate(20.0, 8.0, 20.0)
                  );
                  if (!list.isEmpty()) {
                     LivingEntity livingentity1 = list.get(this.random.nextInt(list.size()));
                     this.setAlternativeTarget(i, livingentity1.getId());
                  }
               }
            }
         }

         if (this.getTarget() != null) {
            this.setAlternativeTarget(0, this.getTarget().getId());
         } else {
            this.setAlternativeTarget(0, 0);
         }

         if (this.destroyBlocksTick > 0) {
            this.destroyBlocksTick--;
            if (this.destroyBlocksTick == 0 && p_363567_.getGameRules().get(GameRules.MOB_GRIEFING)) {
               boolean flag = false;
               int l = Mth.floor(this.getBbWidth() / 2.0F + 1.0F);
               int i1 = Mth.floor(this.getBbHeight());

               for (BlockPos blockpos : BlockPos.betweenClosed(
                  this.getBlockX() - l, this.getBlockY(), this.getBlockZ() - l, this.getBlockX() + l, this.getBlockY() + i1, this.getBlockZ() + l
               )) {
                  BlockState blockstate = p_363567_.getBlockState(blockpos);
                  if (canDestroy(blockstate)) {
                     flag = p_363567_.destroyBlock(blockpos, true, this) || flag;
                  }
               }

               if (flag) {
                  p_363567_.levelEvent(null, 1022, this.blockPosition(), 0);
               }
            }
         }

         if (this.tickCount % 20 == 0) {
            this.heal(1.0F);
         }

         this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
      }
   }

   public static boolean canDestroy(BlockState p_31492_) {
      return !p_31492_.isAir() && !p_31492_.is(BlockTags.WITHER_IMMUNE);
   }

   public void makeInvulnerable() {
      this.setInvulnerableTicks(220);
      this.bossEvent.setProgress(0.0F);
      this.setHealth(this.getMaxHealth() / 3.0F);
   }

   @Override
   public void makeStuckInBlock(BlockState p_31471_, Vec3 p_31472_) {
   }

   @Override
   public void startSeenByPlayer(ServerPlayer p_31483_) {
      super.startSeenByPlayer(p_31483_);
      this.bossEvent.addPlayer(p_31483_);
   }

   @Override
   public void stopSeenByPlayer(ServerPlayer p_31488_) {
      super.stopSeenByPlayer(p_31488_);
      this.bossEvent.removePlayer(p_31488_);
   }

   private double getHeadX(int p_31515_) {
      if (p_31515_ <= 0) {
         return this.getX();
      }

      float f = (this.yBodyRot + 180 * (p_31515_ - 1)) * (float) (Math.PI / 180.0);
      float f1 = Mth.cos(f);
      return this.getX() + f1 * 1.3 * this.getScale();
   }

   private double getHeadY(int p_31517_) {
      float f = p_31517_ <= 0 ? 3.0F : 2.2F;
      return this.getY() + f * this.getScale();
   }

   private double getHeadZ(int p_31519_) {
      if (p_31519_ <= 0) {
         return this.getZ();
      }

      float f = (this.yBodyRot + 180 * (p_31519_ - 1)) * (float) (Math.PI / 180.0);
      float f1 = Mth.sin(f);
      return this.getZ() + f1 * 1.3 * this.getScale();
   }

   private float rotlerp(float p_31443_, float p_31444_, float p_31445_) {
      float f = Mth.wrapDegrees(p_31444_ - p_31443_);
      if (f > p_31445_) {
         f = p_31445_;
      }

      if (f < -p_31445_) {
         f = -p_31445_;
      }

      return p_31443_ + f;
   }

   private void performRangedAttack(int p_31458_, LivingEntity p_31459_) {
      this.performRangedAttack(
         p_31458_, p_31459_.getX(), p_31459_.getY() + p_31459_.getEyeHeight() * 0.5, p_31459_.getZ(), p_31458_ == 0 && this.random.nextFloat() < 0.001F
      );
   }

   private void performRangedAttack(int p_31449_, double p_31450_, double p_31451_, double p_31452_, boolean p_31453_) {
      if (!this.isSilent()) {
         this.level().levelEvent(null, 1024, this.blockPosition(), 0);
      }

      double d0 = this.getHeadX(p_31449_);
      double d1 = this.getHeadY(p_31449_);
      double d2 = this.getHeadZ(p_31449_);
      double d3 = p_31450_ - d0;
      double d4 = p_31451_ - d1;
      double d5 = p_31452_ - d2;
      Vec3 vec3 = new Vec3(d3, d4, d5);
      WitherSkull witherskull = new WitherSkull(this.level(), this, vec3.normalize());
      witherskull.setOwner(this);
      if (p_31453_) {
         witherskull.setDangerous(true);
      }

      witherskull.setPos(d0, d1, d2);
      this.level().addFreshEntity(witherskull);
   }

   @Override
   public void performRangedAttack(LivingEntity p_31468_, float p_31469_) {
      this.performRangedAttack(0, p_31468_);
   }

   @Override
   public boolean hurtServer(ServerLevel p_362291_, DamageSource p_361109_, float p_366425_) {
      if (this.isInvulnerableTo(p_362291_, p_361109_)) {
         return false;
      }

      if (p_361109_.is(DamageTypeTags.WITHER_IMMUNE_TO) || p_361109_.getEntity() instanceof WitherBoss) {
         return false;
      }

      if (this.getInvulnerableTicks() > 0 && !p_361109_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }

      if (this.isPowered()) {
         Entity entity = p_361109_.getDirectEntity();
         if (entity instanceof AbstractArrow || entity instanceof WindCharge) {
            return false;
         }
      }

      Entity entity1 = p_361109_.getEntity();
      if (entity1 != null && entity1.getType().is(EntityTypeTags.WITHER_FRIENDS)) {
         return false;
      }

      if (this.destroyBlocksTick <= 0) {
         this.destroyBlocksTick = 20;
      }

      for (int i = 0; i < this.idleHeadUpdates.length; i++) {
         this.idleHeadUpdates[i] = this.idleHeadUpdates[i] + 3;
      }

      return super.hurtServer(p_362291_, p_361109_, p_366425_);
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel p_342980_, DamageSource p_31464_, boolean p_31466_) {
      super.dropCustomDeathLoot(p_342980_, p_31464_, p_31466_);
      ItemEntity itementity = this.spawnAtLocation(p_342980_, Items.NETHER_STAR);
      if (itementity != null) {
         itementity.setExtendedLifetime();
      }
   }

   @Override
   public void checkDespawn() {
      if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
         this.discard();
      } else {
         this.noActionTime = 0;
      }
   }

   @Override
   public boolean addEffect(MobEffectInstance p_182397_, @Nullable Entity p_182398_) {
      return false;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 300.0)
         .add(Attributes.MOVEMENT_SPEED, 0.6F)
         .add(Attributes.FLYING_SPEED, 0.6F)
         .add(Attributes.FOLLOW_RANGE, 40.0)
         .add(Attributes.ARMOR, 4.0);
   }

   public float[] getHeadYRots() {
      return this.yRotHeads;
   }

   public float[] getHeadXRots() {
      return this.xRotHeads;
   }

   public int getInvulnerableTicks() {
      return this.entityData.get(DATA_ID_INV);
   }

   public void setInvulnerableTicks(int p_31511_) {
      this.entityData.set(DATA_ID_INV, p_31511_);
   }

   public int getAlternativeTarget(int p_31513_) {
      return this.entityData.get(DATA_TARGETS.get(p_31513_));
   }

   public void setAlternativeTarget(int p_31455_, int p_31456_) {
      this.entityData.set(DATA_TARGETS.get(p_31455_), p_31456_);
   }

   public boolean isPowered() {
      return this.getHealth() <= this.getMaxHealth() / 2.0F;
   }

   @Override
   protected boolean canRide(Entity p_31508_) {
      return false;
   }

   @Override
   public boolean canUsePortal(boolean p_342371_) {
      return false;
   }

   @Override
   public boolean canBeAffected(MobEffectInstance p_31495_) {
      return p_31495_.is(MobEffects.WITHER) ? false : super.canBeAffected(p_31495_);
   }

   class WitherDoNothingGoal extends Goal {
      public WitherDoNothingGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         return WitherBoss.this.getInvulnerableTicks() > 0;
      }
   }
}
