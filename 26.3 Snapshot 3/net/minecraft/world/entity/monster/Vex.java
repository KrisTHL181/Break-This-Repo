package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Vex extends Monster implements TraceableEntity, OwnableEntity {
   public static final float FLAP_DEGREES_PER_TICK = 45.836624F;
   public static final int TICKS_PER_FLAP = Mth.ceil((float) (Math.PI * 5.0 / 4.0));
   protected static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Vex.class, EntityDataSerializers.BYTE);
   private static final int FLAG_IS_CHARGING = 1;
   private @Nullable EntityReference<LivingEntity> owner;
   private @Nullable BlockPos boundOrigin;
   private boolean hasLimitedLife;
   private int limitedLifeTicks;

   public Vex(final EntityType<? extends Vex> type, final Level level) {
      super(type, level);
      this.moveControl = new Vex.VexMoveControl<>(this);
      this.xpReward = 3;
   }

   @Override
   public boolean isFlapping() {
      return this.tickCount % TICKS_PER_FLAP == 0;
   }

   @Override
   protected boolean isAffectedByBlocks() {
      return !this.isRemoved();
   }

   @Override
   public void tick() {
      this.noPhysics = true;
      super.tick();
      this.noPhysics = false;
      this.setNoGravity(true);
      if (this.hasLimitedLife && --this.limitedLifeTicks <= 0) {
         this.limitedLifeTicks = 20;
         this.hurt(this.damageSources().starve(), 1.0F);
      }
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(4, new Vex.VexChargeAttackGoal());
      this.goalSelector.addGoal(8, new Vex.VexRandomMoveGoal());
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
      this.targetSelector.addGoal(2, new Vex.VexCopyOwnerTargetGoal(this));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.ATTACK_DAMAGE, 4.0);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_FLAGS_ID, (byte)0);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.boundOrigin = input.<BlockPos>read("bound_pos", BlockPos.CODEC).orElse(null);
      input.getInt("life_ticks").ifPresentOrElse(this::setLimitedLife, () -> this.hasLimitedLife = false);
      this.owner = EntityReference.read(input, "owner");
   }

   @Override
   public void restoreFrom(final Entity oldEntity) {
      super.restoreFrom(oldEntity);
      if (oldEntity instanceof Vex vex) {
         this.owner = vex.owner;
      }
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.storeNullable("bound_pos", BlockPos.CODEC, this.boundOrigin);
      if (this.hasLimitedLife) {
         output.putInt("life_ticks", this.limitedLifeTicks);
      }

      EntityReference.store(this.owner, output, "owner");
   }

   @Override
   public @Nullable LivingEntity getOwner() {
      return OwnableEntity.super.getOwner();
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getOwnerReference() {
      return this.owner;
   }

   public @Nullable BlockPos getBoundOrigin() {
      return this.boundOrigin;
   }

   public void setBoundOrigin(final @Nullable BlockPos boundOrigin) {
      this.boundOrigin = boundOrigin;
   }

   private boolean getVexFlag(final int flag) {
      int flags = this.entityData.get(DATA_FLAGS_ID);
      return (flags & flag) != 0;
   }

   private void setVexFlag(final int flag, final boolean value) {
      int flags = this.entityData.get(DATA_FLAGS_ID);
      if (value) {
         flags |= flag;
      } else {
         flags &= ~flag;
      }

      this.entityData.set(DATA_FLAGS_ID, (byte)(flags & 0xFF));
   }

   public boolean isCharging() {
      return this.getVexFlag(1);
   }

   public void setIsCharging(final boolean value) {
      this.setVexFlag(1, value);
   }

   public void setOwner(final Mob owner) {
      this.owner = EntityReference.of(owner);
   }

   public void setLimitedLife(final int lifeTicks) {
      this.hasLimitedLife = true;
      this.limitedLifeTicks = lifeTicks;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.VEX_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.VEX_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.VEX_HURT;
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      RandomSource random = level.getRandom();
      this.populateDefaultEquipmentSlots(random, difficulty);
      this.populateDefaultEquipmentEnchantments(level, random, difficulty);
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   @Override
   protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
      this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   private class VexChargeAttackGoal extends Goal {
      public VexChargeAttackGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         LivingEntity target = Vex.this.getTarget();
         return target != null && target.isAlive() && !Vex.this.getMoveControl().hasWanted() && Vex.this.random.nextInt(reducedTickDelay(7)) == 0
            ? Vex.this.distanceToSqr(target) > 4.0
            : false;
      }

      @Override
      public boolean canContinueToUse() {
         return Vex.this.getMoveControl().hasWanted() && Vex.this.isCharging() && Vex.this.getTarget() != null && Vex.this.getTarget().isAlive();
      }

      @Override
      public void start() {
         LivingEntity attackTarget = Vex.this.getTarget();
         if (attackTarget != null) {
            Vec3 eyePosition = attackTarget.getEyePosition();
            Vex.this.moveControl.setWantedPosition(eyePosition.x, eyePosition.y, eyePosition.z, 1.0);
         }

         Vex.this.setIsCharging(true);
         Vex.this.playSound(SoundEvents.VEX_CHARGE, 1.0F, 1.0F);
      }

      @Override
      public void stop() {
         Vex.this.setIsCharging(false);
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         LivingEntity attackTarget = Vex.this.getTarget();
         if (attackTarget != null) {
            if (Vex.this.getBoundingBox().intersects(attackTarget.getBoundingBox())) {
               Vex.this.doHurtTarget(getServerLevel(Vex.this.level()), attackTarget);
               Vex.this.setIsCharging(false);
            } else {
               double distance = Vex.this.distanceToSqr(attackTarget);
               if (distance < 9.0) {
                  Vec3 eyePosition = attackTarget.getEyePosition();
                  Vex.this.moveControl.setWantedPosition(eyePosition.x, eyePosition.y, eyePosition.z, 1.0);
               }
            }
         }
      }
   }

   private class VexCopyOwnerTargetGoal extends TargetGoal {
      private final TargetingConditions copyOwnerTargeting = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

      public VexCopyOwnerTargetGoal(final PathfinderMob mob) {
         super(mob, false);
      }

      @Override
      public boolean canUse() {
         return Vex.this.getOwner() instanceof Targeting owner && owner.getTarget() != null && this.canAttack(owner.getTarget(), this.copyOwnerTargeting);
      }

      @Override
      public void start() {
         Vex.this.setTarget(Vex.this.getOwner() instanceof Targeting owner ? owner.getTarget() : null);
         super.start();
      }
   }

   private class VexMoveControl<T extends Mob> extends MoveControl<T> {
      public VexMoveControl(final T vex) {
         super(vex);
      }

      @Override
      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO) {
            Vec3 delta = new Vec3(this.wantedX - Vex.this.getX(), this.wantedY - Vex.this.getY(), this.wantedZ - Vex.this.getZ());
            double deltaLength = delta.length();
            if (deltaLength < Vex.this.getBoundingBox().getSize()) {
               this.operation = MoveControl.Operation.WAIT;
               Vex.this.setDeltaMovement(Vex.this.getDeltaMovement().scale(0.5));
            } else {
               Vex.this.setDeltaMovement(Vex.this.getDeltaMovement().add(delta.scale(this.speedModifier * 0.05 / deltaLength)));
               if (Vex.this.getTarget() == null) {
                  Vec3 movement = Vex.this.getDeltaMovement();
                  Vex.this.setYRot(-((float)Mth.atan2(movement.x, movement.z)) * (180.0F / (float)Math.PI));
                  Vex.this.yBodyRot = Vex.this.getYRot();
               } else {
                  double tx = Vex.this.getTarget().getX() - Vex.this.getX();
                  double tz = Vex.this.getTarget().getZ() - Vex.this.getZ();
                  Vex.this.setYRot(-((float)Mth.atan2(tx, tz)) * (180.0F / (float)Math.PI));
                  Vex.this.yBodyRot = Vex.this.getYRot();
               }
            }
         }
      }
   }

   private class VexRandomMoveGoal extends Goal {
      public VexRandomMoveGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         return !Vex.this.getMoveControl().hasWanted() && Vex.this.random.nextInt(reducedTickDelay(7)) == 0;
      }

      @Override
      public boolean canContinueToUse() {
         return false;
      }

      @Override
      public void tick() {
         BlockPos boundOrigin = Vex.this.getBoundOrigin();
         if (boundOrigin == null) {
            boundOrigin = Vex.this.blockPosition();
         }

         for (int attempts = 0; attempts < 3; attempts++) {
            BlockPos testPos = boundOrigin.offset(Vex.this.random.nextInt(15) - 7, Vex.this.random.nextInt(11) - 5, Vex.this.random.nextInt(15) - 7);
            if (Vex.this.level().isEmptyBlock(testPos)) {
               Vex.this.moveControl.setWantedPosition(testPos.getX() + 0.5, testPos.getY() + 0.5, testPos.getZ() + 0.5, 0.25);
               if (Vex.this.getTarget() == null) {
                  Vex.this.getLookControl().setLookAt(testPos.getX() + 0.5, testPos.getY() + 0.5, testPos.getZ() + 0.5, 180.0F, 20.0F);
               }
               break;
            }
         }
      }
   }
}
