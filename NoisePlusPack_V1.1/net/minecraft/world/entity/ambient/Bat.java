package net.minecraft.world.entity.ambient;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Bat extends AmbientCreature {
   public static final float FLAP_LENGTH_SECONDS = 0.5F;
   public static final float TICKS_PER_FLAP = 10.0F;
   private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(Bat.class, EntityDataSerializers.BYTE);
   private static final int FLAG_RESTING = 1;
   private static final TargetingConditions BAT_RESTING_TARGETING = TargetingConditions.forNonCombat().range(4.0);
   private static final byte DEFAULT_FLAGS = 0;
   public final AnimationState flyAnimationState = new AnimationState();
   public final AnimationState restAnimationState = new AnimationState();
   private @Nullable BlockPos targetPosition;

   public Bat(EntityType<? extends Bat> p_27412_, Level p_27413_) {
      super(p_27412_, p_27413_);
      if (!p_27413_.isClientSide()) {
         this.setResting(true);
      }
   }

   @Override
   public boolean isFlapping() {
      return !this.isResting() && this.tickCount % 10.0F == 0.0F;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_332675_) {
      super.defineSynchedData(p_332675_);
      p_332675_.define(DATA_ID_FLAGS, (byte)0);
   }

   @Override
   protected float getSoundVolume() {
      return 0.1F;
   }

   @Override
   public float getVoicePitch() {
      return super.getVoicePitch() * 0.95F;
   }

   @Override
   public @Nullable SoundEvent getAmbientSound() {
      return this.isResting() && this.random.nextInt(4) != 0 ? null : SoundEvents.BAT_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_27451_) {
      return SoundEvents.BAT_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.BAT_DEATH;
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   protected void doPush(Entity p_27415_) {
   }

   @Override
   protected void pushEntities() {
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0);
   }

   public boolean isResting() {
      return (this.entityData.get(DATA_ID_FLAGS) & 1) != 0;
   }

   public void setResting(boolean p_27457_) {
      byte b0 = this.entityData.get(DATA_ID_FLAGS);
      if (p_27457_) {
         this.entityData.set(DATA_ID_FLAGS, (byte)(b0 | 1));
      } else {
         this.entityData.set(DATA_ID_FLAGS, (byte)(b0 & -2));
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isResting()) {
         this.setDeltaMovement(Vec3.ZERO);
         this.setPosRaw(this.getX(), Mth.floor(this.getY()) + 1.0 - this.getBbHeight(), this.getZ());
      } else {
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
      }

      this.setupAnimationStates();
   }

   @Override
   protected void customServerAiStep(ServerLevel p_369019_) {
      super.customServerAiStep(p_369019_);
      BlockPos blockpos = this.blockPosition();
      BlockPos blockpos1 = blockpos.above();
      if (this.isResting()) {
         boolean flag = this.isSilent();
         if (p_369019_.getBlockState(blockpos1).isRedstoneConductor(p_369019_, blockpos)) {
            if (this.random.nextInt(200) == 0) {
               this.yHeadRot = this.random.nextInt(360);
            }

            if (p_369019_.getNearestPlayer(BAT_RESTING_TARGETING, this) != null) {
               this.setResting(false);
               if (!flag) {
                  p_369019_.levelEvent(null, 1025, blockpos, 0);
               }
            }
         } else {
            this.setResting(false);
            if (!flag) {
               p_369019_.levelEvent(null, 1025, blockpos, 0);
            }
         }
      } else {
         if (this.targetPosition != null && (!p_369019_.isEmptyBlock(this.targetPosition) || this.targetPosition.getY() <= p_369019_.getMinY())) {
            this.targetPosition = null;
         }

         if (this.targetPosition == null || this.random.nextInt(30) == 0 || this.targetPosition.closerToCenterThan(this.position(), 2.0)) {
            this.targetPosition = BlockPos.containing(
               this.getX() + this.random.nextInt(7) - this.random.nextInt(7),
               this.getY() + this.random.nextInt(6) - 2.0,
               this.getZ() + this.random.nextInt(7) - this.random.nextInt(7)
            );
         }

         double d2 = this.targetPosition.getX() + 0.5 - this.getX();
         double d0 = this.targetPosition.getY() + 0.1 - this.getY();
         double d1 = this.targetPosition.getZ() + 0.5 - this.getZ();
         Vec3 vec3 = this.getDeltaMovement();
         Vec3 vec31 = vec3.add((Math.signum(d2) * 0.5 - vec3.x) * 0.1F, (Math.signum(d0) * 0.7F - vec3.y) * 0.1F, (Math.signum(d1) * 0.5 - vec3.z) * 0.1F);
         this.setDeltaMovement(vec31);
         float f = (float)(Mth.atan2(vec31.z, vec31.x) * 180.0 / (float) Math.PI) - 90.0F;
         float f1 = Mth.wrapDegrees(f - this.getYRot());
         this.zza = 0.5F;
         this.setYRot(this.getYRot() + f1);
         if (this.random.nextInt(100) == 0 && p_369019_.getBlockState(blockpos1).isRedstoneConductor(p_369019_, blockpos1)) {
            this.setResting(true);
         }
      }
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   protected void checkFallDamage(double p_27419_, boolean p_27420_, BlockState p_27421_, BlockPos p_27422_) {
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   @Override
   public boolean hurtServer(ServerLevel p_361230_, DamageSource p_366357_, float p_366075_) {
      if (this.isInvulnerableTo(p_361230_, p_366357_)) {
         return false;
      }

      if (this.isResting()) {
         this.setResting(false);
      }

      return super.hurtServer(p_361230_, p_366357_, p_366075_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406358_) {
      super.readAdditionalSaveData(p_406358_);
      this.entityData.set(DATA_ID_FLAGS, p_406358_.getByteOr("BatFlags", (byte)0));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_405897_) {
      super.addAdditionalSaveData(p_405897_);
      p_405897_.putByte("BatFlags", this.entityData.get(DATA_ID_FLAGS));
   }

   public static boolean checkBatSpawnRules(
      EntityType<Bat> p_218099_, LevelAccessor p_218100_, EntitySpawnReason p_364019_, BlockPos p_218102_, RandomSource p_218103_
   ) {
      if (p_218102_.getY() >= p_218100_.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, p_218102_).getY()) {
         return false;
      } else if (p_218103_.nextBoolean()) {
         return false;
      } else if (p_218100_.getMaxLocalRawBrightness(p_218102_) > p_218103_.nextInt(4)) {
         return false;
      } else {
         return !p_218100_.getBlockState(p_218102_.below()).is(BlockTags.BATS_SPAWNABLE_ON)
            ? false
            : checkMobSpawnRules(p_218099_, p_218100_, p_364019_, p_218102_, p_218103_);
      }
   }

   private void setupAnimationStates() {
      if (this.isResting()) {
         this.flyAnimationState.stop();
         this.restAnimationState.startIfStopped(this.tickCount);
      } else {
         this.restAnimationState.stop();
         this.flyAnimationState.startIfStopped(this.tickCount);
      }
   }
}
