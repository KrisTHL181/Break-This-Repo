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
import net.minecraft.world.entity.EntityTypes;
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

   public IronGolem(final EntityType<? extends IronGolem> type, final Level level) {
      super(type, level);
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
            3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false, (target, var1) -> target instanceof Enemy && !(target instanceof Creeper))
         );
      this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_FLAGS_ID, (byte)0);
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
   protected int decreaseAirSupply(final int currentSupply) {
      return currentSupply;
   }

   @Override
   protected void doPush(final Entity entity) {
      if (entity instanceof Enemy && !(entity instanceof Creeper) && this.getRandom().nextInt(20) == 0) {
         this.setTarget((LivingEntity)entity);
      }

      super.doPush(entity);
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
   public boolean canAttack(final LivingEntity target) {
      if (this.isPlayerCreated() && target.is(EntityTypes.PLAYER)) {
         return false;
      } else {
         return target.is(EntityTypes.CREEPER) ? false : super.canAttack(target);
      }
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("PlayerCreated", this.isPlayerCreated());
      this.addPersistentAngerSaveData(output);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setPlayerCreated(input.getBooleanOr("PlayerCreated", false));
      this.readPersistentAngerSaveData(this.level(), input);
   }

   @Override
   public void startPersistentAngerTimer() {
      this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   @Override
   public void setPersistentAngerEndTime(final long endTime) {
      this.persistentAngerEndTime = endTime;
   }

   @Override
   public long getPersistentAngerEndTime() {
      return this.persistentAngerEndTime;
   }

   @Override
   public void setPersistentAngerTarget(final @Nullable EntityReference<LivingEntity> persistentAngerTarget) {
      this.persistentAngerTarget = persistentAngerTarget;
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   private float getAttackDamage() {
      return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
   }

   @Override
   public boolean doHurtTarget(final ServerLevel level, final Entity target) {
      this.attackAnimationTick = 10;
      level.broadcastEntityEvent(this, (byte)4);
      float attackDamage = this.getAttackDamage();
      float damage = (int)attackDamage > 0 ? attackDamage / 2.0F + this.random.nextInt((int)attackDamage) : attackDamage;
      DamageSource damageSource = this.damageSources().mobAttack(this);
      boolean hurt = target.hurtServer(level, damageSource, damage);
      if (hurt) {
         double knockbackResistance = target instanceof LivingEntity livingEntity ? livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0;
         double scale = Math.max(0.0, 1.0 - knockbackResistance);
         target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4F * scale, 0.0));
         EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
      }

      this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
      return hurt;
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      Crackiness.Level previousCrackiness = this.getCrackiness();
      boolean wasHurt = super.hurtServer(level, source, damage);
      if (wasHurt && this.getCrackiness() != previousCrackiness) {
         this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 1.0F);
      }

      return wasHurt;
   }

   public Crackiness.Level getCrackiness() {
      return Crackiness.GOLEM.byFraction(this.getHealth() / this.getMaxHealth());
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 4) {
         this.attackAnimationTick = 10;
         this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
      } else if (id == 11) {
         this.offerFlowerTick = 400;
      } else if (id == 34) {
         this.offerFlowerTick = 0;
      } else {
         super.handleEntityEvent(id);
      }
   }

   public int getAttackAnimationTick() {
      return this.attackAnimationTick;
   }

   public void offerFlower(final boolean offer) {
      if (offer) {
         this.offerFlowerTick = 400;
         this.level().broadcastEntityEvent(this, (byte)11);
      } else {
         this.offerFlowerTick = 0;
         this.level().broadcastEntityEvent(this, (byte)34);
      }
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   @Override
   protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (!itemStack.is(Items.IRON_INGOT)) {
         return InteractionResult.PASS;
      }

      float healthBefore = this.getHealth();
      this.heal(25.0F);
      if (this.getHealth() == healthBefore) {
         return InteractionResult.PASS;
      }

      float pitch = 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F;
      this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, pitch);
      itemStack.consume(1, player);
      return InteractionResult.SUCCESS;
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
   }

   public int getOfferFlowerTick() {
      return this.offerFlowerTick;
   }

   public boolean isPlayerCreated() {
      return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   public void setPlayerCreated(final boolean value) {
      byte current = this.entityData.get(DATA_FLAGS_ID);
      if (value) {
         this.entityData.set(DATA_FLAGS_ID, (byte)(current | 1));
      } else {
         this.entityData.set(DATA_FLAGS_ID, (byte)(current & -2));
      }
   }

   @Override
   public void die(final DamageSource source) {
      super.die(source);
   }

   @Override
   public boolean checkSpawnObstruction(final LevelReader level) {
      BlockPos pos = this.blockPosition();
      BlockPos belowPos = pos.below();
      BlockState below = level.getBlockState(belowPos);
      if (!below.entityCanStandOn(level, belowPos, this)) {
         return false;
      }

      for (int i = 1; i < 3; i++) {
         BlockPos abovePos = pos.above(i);
         BlockState above = level.getBlockState(abovePos);
         if (!NaturalSpawner.isValidEmptySpawnBlock(level, abovePos, above, above.getFluidState(), EntityTypes.IRON_GOLEM)) {
            return false;
         }
      }

      return NaturalSpawner.isValidEmptySpawnBlock(level, pos, level.getBlockState(pos), Fluids.EMPTY.defaultFluidState(), EntityTypes.IRON_GOLEM)
         && level.isUnobstructed(this);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.875F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }
}
