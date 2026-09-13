package net.minecraft.world.entity.animal.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.OfferFlowerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class IronGolem extends AbstractGolem implements NeutralMob {
   protected static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(IronGolem.class, EntityDataSerializers.BYTE);
   private static final int IRON_INGOT_HEAL_AMOUNT = 25;
   private static final boolean DEFAULT_PLAYER_CREATED = false;
   private int attackAnimationTick;
   private int offerFlowerTick;
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   private long persistentAngerEndTime;
   private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

   public IronGolem(EntityType<? extends IronGolem> p_457864_, Level p_460546_) {
      super(p_457864_, p_460546_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
      this.goalSelector.addGoal(2, new MoveBackToVillageGoal(this, 0.6, false));
      this.goalSelector.addGoal(4, new GolemRandomStrollInVillageGoal(this, 0.6));
      this.goalSelector.addGoal(5, new OfferFlowerGoal(this));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new DefendVillageTargetGoal(this));
      this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
      this.targetSelector
         .addGoal(
            3,
            new NearestAttackableTargetGoal<>(
               this, Mob.class, 5, false, false, (p_450611_, p_455262_) -> p_450611_ instanceof Enemy && !(p_450611_ instanceof Creeper)
            )
         );
      this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_453368_) {
      super.defineSynchedData(p_453368_);
      p_453368_.define(DATA_FLAGS_ID, (byte)0);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 100.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ATTACK_DAMAGE, 15.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected int decreaseAirSupply(int p_454554_) {
      return p_454554_;
   }

   @Override
   protected void doPush(Entity p_455629_) {
      if (p_455629_ instanceof Enemy && !(p_455629_ instanceof Creeper) && this.getRandom().nextInt(20) == 0) {
         this.setTarget((LivingEntity)p_455629_);
      }

      super.doPush(p_455629_);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.attackAnimationTick > 0) {
         this.attackAnimationTick--;
      }

      if (this.offerFlowerTick > 0) {
         this.offerFlowerTick--;
      }

      if (!this.level().isClientSide()) {
         this.updatePersistentAnger((ServerLevel)this.level(), true);
      }
   }

   @Override
   public boolean canSpawnSprintParticle() {
      return this.getDeltaMovement().horizontalDistanceSqr() > 2.5000003E-7F && this.random.nextInt(5) == 0;
   }

   @Override
   public boolean canAttackType(EntityType<?> p_458463_) {
      if (this.isPlayerCreated() && p_458463_ == EntityType.PLAYER) {
         return false;
      } else {
         return p_458463_ == EntityType.CREEPER ? false : super.canAttackType(p_458463_);
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_451586_) {
      super.addAdditionalSaveData(p_451586_);
      p_451586_.putBoolean("PlayerCreated", this.isPlayerCreated());
      this.addPersistentAngerSaveData(p_451586_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_459264_) {
      super.readAdditionalSaveData(p_459264_);
      this.setPlayerCreated(p_459264_.getBooleanOr("PlayerCreated", false));
      this.readPersistentAngerSaveData(this.level(), p_459264_);
   }

   @Override
   public void startPersistentAngerTimer() {
      this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   @Override
   public void setPersistentAngerEndTime(long p_459658_) {
      this.persistentAngerEndTime = p_459658_;
   }

   @Override
   public long getPersistentAngerEndTime() {
      return this.persistentAngerEndTime;
   }

   @Override
   public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> p_458243_) {
      this.persistentAngerTarget = p_458243_;
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   private float getAttackDamage() {
      return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_454432_, Entity p_455370_) {
      this.attackAnimationTick = 10;
      p_454432_.broadcastEntityEvent(this, (byte)4);
      float f = this.getAttackDamage();
      float f1 = (int)f > 0 ? f / 2.0F + this.random.nextInt((int)f) : f;
      DamageSource damagesource = this.damageSources().mobAttack(this);
      boolean flag = p_455370_.hurtServer(p_454432_, damagesource, f1);
      if (flag) {
         double d0 = p_455370_ instanceof LivingEntity livingentity ? livingentity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0;
         double d1 = Math.max(0.0, 1.0 - d0);
         p_455370_.setDeltaMovement(p_455370_.getDeltaMovement().add(0.0, 0.4F * d1, 0.0));
         EnchantmentHelper.doPostAttackEffects(p_454432_, p_455370_, damagesource);
      }

      this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
      return flag;
   }

   @Override
   public boolean hurtServer(ServerLevel p_452537_, DamageSource p_454647_, float p_458618_) {
      Crackiness.Level crackiness$level = this.getCrackiness();
      boolean flag = super.hurtServer(p_452537_, p_454647_, p_458618_);
      if (flag && this.getCrackiness() != crackiness$level) {
         this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 1.0F);
      }

      return flag;
   }

   public Crackiness.Level getCrackiness() {
      return Crackiness.GOLEM.byFraction(this.getHealth() / this.getMaxHealth());
   }

   @Override
   public void handleEntityEvent(byte p_459954_) {
      if (p_459954_ == 4) {
         this.attackAnimationTick = 10;
         this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
      } else if (p_459954_ == 11) {
         this.offerFlowerTick = 400;
      } else if (p_459954_ == 34) {
         this.offerFlowerTick = 0;
      } else {
         super.handleEntityEvent(p_459954_);
      }
   }

   public int getAttackAnimationTick() {
      return this.attackAnimationTick;
   }

   public void offerFlower(boolean p_457857_) {
      if (p_457857_) {
         this.offerFlowerTick = 400;
         this.level().broadcastEntityEvent(this, (byte)11);
      } else {
         this.offerFlowerTick = 0;
         this.level().broadcastEntityEvent(this, (byte)34);
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_457145_) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   @Override
   protected InteractionResult mobInteract(Player p_460399_, InteractionHand p_457569_) {
      ItemStack itemstack = p_460399_.getItemInHand(p_457569_);
      if (!itemstack.is(Items.IRON_INGOT)) {
         return InteractionResult.PASS;
      }

      float f = this.getHealth();
      this.heal(25.0F);
      if (this.getHealth() == f) {
         return InteractionResult.PASS;
      }

      float f1 = 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F;
      this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, f1);
      itemstack.consume(1, p_460399_);
      return InteractionResult.SUCCESS;
   }

   @Override
   protected void playStepSound(BlockPos p_460451_, BlockState p_451787_) {
      this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
   }

   public int getOfferFlowerTick() {
      return this.offerFlowerTick;
   }

   public boolean isPlayerCreated() {
      return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   public void setPlayerCreated(boolean p_453593_) {
      byte b0 = this.entityData.get(DATA_FLAGS_ID);
      if (p_453593_) {
         this.entityData.set(DATA_FLAGS_ID, (byte)(b0 | 1));
      } else {
         this.entityData.set(DATA_FLAGS_ID, (byte)(b0 & -2));
      }
   }

   @Override
   public void die(DamageSource p_451513_) {
      super.die(p_451513_);
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_459802_) {
      BlockPos blockpos = this.blockPosition();
      BlockPos blockpos1 = blockpos.below();
      BlockState blockstate = p_459802_.getBlockState(blockpos1);
      if (!blockstate.entityCanStandOn(p_459802_, blockpos1, this)) {
         return false;
      }

      for (int i = 1; i < 3; i++) {
         BlockPos blockpos2 = blockpos.above(i);
         BlockState blockstate1 = p_459802_.getBlockState(blockpos2);
         if (!NaturalSpawner.isValidEmptySpawnBlock(p_459802_, blockpos2, blockstate1, blockstate1.getFluidState(), EntityType.IRON_GOLEM)) {
            return false;
         }
      }

      return NaturalSpawner.isValidEmptySpawnBlock(
            p_459802_, blockpos, p_459802_.getBlockState(blockpos), Fluids.EMPTY.defaultFluidState(), EntityType.IRON_GOLEM
         )
         && p_459802_.isUnobstructed(this);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.875F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }
}
