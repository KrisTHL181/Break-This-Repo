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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
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

public class Vex extends Monster implements TraceableEntity {
   public static final float FLAP_DEGREES_PER_TICK = 45.836624F;
   public static final int TICKS_PER_FLAP = Mth.ceil((float) (Math.PI * 5.0 / 4.0));
   protected static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Vex.class, EntityDataSerializers.BYTE);
   private static final int FLAG_IS_CHARGING = 1;
   private @Nullable EntityReference<Mob> owner;
   private @Nullable BlockPos boundOrigin;
   private boolean hasLimitedLife;
   private int limitedLifeTicks;

   public Vex(EntityType<? extends Vex> p_33984_, Level p_33985_) {
      super(p_33984_, p_33985_);
      this.moveControl = new Vex.VexMoveControl(this);
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
   protected void defineSynchedData(SynchedEntityData.Builder p_336017_) {
      super.defineSynchedData(p_336017_);
      p_336017_.define(DATA_FLAGS_ID, (byte)0);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_407021_) {
      super.readAdditionalSaveData(p_407021_);
      this.boundOrigin = p_407021_.<BlockPos>read("bound_pos", BlockPos.CODEC).orElse(null);
      p_407021_.getInt("life_ticks").ifPresentOrElse(this::setLimitedLife, () -> this.hasLimitedLife = false);
      this.owner = EntityReference.read(p_407021_, "owner");
   }

   @Override
   public void restoreFrom(Entity p_309610_) {
      super.restoreFrom(p_309610_);
      if (p_309610_ instanceof Vex vex) {
         this.owner = vex.owner;
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_408813_) {
      super.addAdditionalSaveData(p_408813_);
      p_408813_.storeNullable("bound_pos", BlockPos.CODEC, this.boundOrigin);
      if (this.hasLimitedLife) {
         p_408813_.putInt("life_ticks", this.limitedLifeTicks);
      }

      EntityReference.store(this.owner, p_408813_, "owner");
   }

   public @Nullable Mob getOwner() {
      return EntityReference.get(this.owner, this.level(), Mob.class);
   }

   public @Nullable BlockPos getBoundOrigin() {
      return this.boundOrigin;
   }

   public void setBoundOrigin(@Nullable BlockPos p_34034_) {
      this.boundOrigin = p_34034_;
   }

   private boolean getVexFlag(int p_34011_) {
      int i = this.entityData.get(DATA_FLAGS_ID);
      return (i & p_34011_) != 0;
   }

   private void setVexFlag(int p_33990_, boolean p_33991_) {
      int i = this.entityData.get(DATA_FLAGS_ID);
      if (p_33991_) {
         i |= p_33990_;
      } else {
         i &= ~p_33990_;
      }

      this.entityData.set(DATA_FLAGS_ID, (byte)(i & 0xFF));
   }

   public boolean isCharging() {
      return this.getVexFlag(1);
   }

   public void setIsCharging(boolean p_34043_) {
      this.setVexFlag(1, p_34043_);
   }

   public void setOwner(Mob p_33995_) {
      this.owner = EntityReference.of(p_33995_);
   }

   public void setLimitedLife(int p_33988_) {
      this.hasLimitedLife = true;
      this.limitedLifeTicks = p_33988_;
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
   protected SoundEvent getHurtSound(DamageSource p_34023_) {
      return SoundEvents.VEX_HURT;
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_34002_, DifficultyInstance p_34003_, EntitySpawnReason p_366057_, @Nullable SpawnGroupData p_34005_
   ) {
      RandomSource randomsource = p_34002_.getRandom();
      this.populateDefaultEquipmentSlots(randomsource, p_34003_);
      this.populateDefaultEquipmentEnchantments(p_34002_, randomsource, p_34003_);
      return super.finalizeSpawn(p_34002_, p_34003_, p_366057_, p_34005_);
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_219135_, DifficultyInstance p_219136_) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
      this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   class VexChargeAttackGoal extends Goal {
      public VexChargeAttackGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         LivingEntity livingentity = Vex.this.getTarget();
         return livingentity != null && livingentity.isAlive() && !Vex.this.getMoveControl().hasWanted() && Vex.this.random.nextInt(reducedTickDelay(7)) == 0
            ? Vex.this.distanceToSqr(livingentity) > 4.0
            : false;
      }

      @Override
      public boolean canContinueToUse() {
         return Vex.this.getMoveControl().hasWanted() && Vex.this.isCharging() && Vex.this.getTarget() != null && Vex.this.getTarget().isAlive();
      }

      @Override
      public void start() {
         LivingEntity livingentity = Vex.this.getTarget();
         if (livingentity != null) {
            Vec3 vec3 = livingentity.getEyePosition();
            Vex.this.moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, 1.0);
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
         LivingEntity livingentity = Vex.this.getTarget();
         if (livingentity != null) {
            if (Vex.this.getBoundingBox().intersects(livingentity.getBoundingBox())) {
               Vex.this.doHurtTarget(getServerLevel(Vex.this.level()), livingentity);
               Vex.this.setIsCharging(false);
            } else {
               double d0 = Vex.this.distanceToSqr(livingentity);
               if (d0 < 9.0) {
                  Vec3 vec3 = livingentity.getEyePosition();
                  Vex.this.moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, 1.0);
               }
            }
         }
      }
   }

   class VexCopyOwnerTargetGoal extends TargetGoal {
      private final TargetingConditions copyOwnerTargeting = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

      public VexCopyOwnerTargetGoal(final PathfinderMob p_34056_) {
         super(p_34056_, false);
      }

      @Override
      public boolean canUse() {
         Mob mob = Vex.this.getOwner();
         return mob != null && mob.getTarget() != null && this.canAttack(mob.getTarget(), this.copyOwnerTargeting);
      }

      @Override
      public void start() {
         Mob mob = Vex.this.getOwner();
         Vex.this.setTarget(mob != null ? mob.getTarget() : null);
         super.start();
      }
   }

   class VexMoveControl extends MoveControl {
      public VexMoveControl(final Vex p_34062_) {
         super(p_34062_);
      }

      @Override
      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO) {
            Vec3 vec3 = new Vec3(this.wantedX - Vex.this.getX(), this.wantedY - Vex.this.getY(), this.wantedZ - Vex.this.getZ());
            double d0 = vec3.length();
            if (d0 < Vex.this.getBoundingBox().getSize()) {
               this.operation = MoveControl.Operation.WAIT;
               Vex.this.setDeltaMovement(Vex.this.getDeltaMovement().scale(0.5));
            } else {
               Vex.this.setDeltaMovement(Vex.this.getDeltaMovement().add(vec3.scale(this.speedModifier * 0.05 / d0)));
               if (Vex.this.getTarget() == null) {
                  Vec3 vec31 = Vex.this.getDeltaMovement();
                  Vex.this.setYRot(-((float)Mth.atan2(vec31.x, vec31.z)) * (180.0F / (float)Math.PI));
                  Vex.this.yBodyRot = Vex.this.getYRot();
               } else {
                  double d2 = Vex.this.getTarget().getX() - Vex.this.getX();
                  double d1 = Vex.this.getTarget().getZ() - Vex.this.getZ();
                  Vex.this.setYRot(-((float)Mth.atan2(d2, d1)) * (180.0F / (float)Math.PI));
                  Vex.this.yBodyRot = Vex.this.getYRot();
               }
            }
         }
      }
   }

   class VexRandomMoveGoal extends Goal {
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
         BlockPos blockpos = Vex.this.getBoundOrigin();
         if (blockpos == null) {
            blockpos = Vex.this.blockPosition();
         }

         for (int i = 0; i < 3; i++) {
            BlockPos blockpos1 = blockpos.offset(Vex.this.random.nextInt(15) - 7, Vex.this.random.nextInt(11) - 5, Vex.this.random.nextInt(15) - 7);
            if (Vex.this.level().isEmptyBlock(blockpos1)) {
               Vex.this.moveControl.setWantedPosition(blockpos1.getX() + 0.5, blockpos1.getY() + 0.5, blockpos1.getZ() + 0.5, 0.25);
               if (Vex.this.getTarget() == null) {
                  Vex.this.getLookControl().setLookAt(blockpos1.getX() + 0.5, blockpos1.getY() + 0.5, blockpos1.getZ() + 0.5, 180.0F, 20.0F);
               }
               break;
            }
         }
      }
   }
}
