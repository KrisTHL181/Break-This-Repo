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

   public Bat(final EntityType<? extends Bat> type, final Level level) {
      super(type, level);
      if (!level.isClientSide()) {
         this.setResting(true);
      }
   }

   @Override
   public boolean isFlapping() {
      return !this.isResting() && this.tickCount % 10.0F == 0.0F;
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_ID_FLAGS, (byte)0);
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
   protected SoundEvent getHurtSound(final DamageSource source) {
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
   protected void doPush(final Entity entity) {
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

   public void setResting(final boolean value) {
      byte current = this.entityData.get(DATA_ID_FLAGS);
      if (value) {
         this.entityData.set(DATA_ID_FLAGS, (byte)(current | 1));
      } else {
         this.entityData.set(DATA_ID_FLAGS, (byte)(current & -2));
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
   protected void customServerAiStep(final ServerLevel level) {
      super.customServerAiStep(level);
      BlockPos pos = this.blockPosition();
      BlockPos above = pos.above();
      if (this.isResting()) {
         boolean isSilent = this.isSilent();
         if (level.getBlockState(above).isRedstoneConductor(level, pos)) {
            if (this.random.nextInt(200) == 0) {
               this.yHeadRot = this.random.nextInt(360);
            }

            if (level.getNearestPlayer(BAT_RESTING_TARGETING, this) != null) {
               this.setResting(false);
               if (!isSilent) {
                  level.levelEvent(null, 1025, pos, 0);
               }
            }
         } else {
            this.setResting(false);
            if (!isSilent) {
               level.levelEvent(null, 1025, pos, 0);
            }
         }
      } else {
         if (this.targetPosition != null && (!level.isEmptyBlock(this.targetPosition) || this.targetPosition.getY() <= level.getMinY())) {
            this.targetPosition = null;
         }

         if (this.targetPosition == null || this.random.nextInt(30) == 0 || this.targetPosition.closerToCenterThan(this.position(), 2.0)) {
            this.targetPosition = BlockPos.containing(
               this.getX() + this.random.nextInt(7) - this.random.nextInt(7),
               this.getY() + this.random.nextInt(6) - 2.0,
               this.getZ() + this.random.nextInt(7) - this.random.nextInt(7)
            );
         }

         double dx = this.targetPosition.getX() + 0.5 - this.getX();
         double dy = this.targetPosition.getY() + 0.1 - this.getY();
         double dz = this.targetPosition.getZ() + 0.5 - this.getZ();
         Vec3 movement = this.getDeltaMovement();
         Vec3 newMovement = movement.add(
            (Math.signum(dx) * 0.5 - movement.x) * 0.1F, (Math.signum(dy) * 0.7F - movement.y) * 0.1F, (Math.signum(dz) * 0.5 - movement.z) * 0.1F
         );
         this.setDeltaMovement(newMovement);
         float yRotD = (float)(Mth.atan2(newMovement.z, newMovement.x) * 180.0F / (float)Math.PI) - 90.0F;
         float rotDiff = Mth.wrapDegrees(yRotD - this.getYRot());
         this.zza = 0.5F;
         this.setYRot(this.getYRot() + rotDiff);
         if (this.random.nextInt(100) == 0 && level.getBlockState(above).isRedstoneConductor(level, above)) {
            this.setResting(true);
         }
      }
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      if (this.isInvulnerableTo(level, source)) {
         return false;
      }

      if (this.isResting()) {
         this.setResting(false);
      }

      return super.hurtServer(level, source, damage);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.entityData.set(DATA_ID_FLAGS, input.getByteOr("BatFlags", (byte)0));
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putByte("BatFlags", this.entityData.get(DATA_ID_FLAGS));
   }

   public static boolean checkBatSpawnRules(
      final EntityType<Bat> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      if (pos.getY() >= level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos).getY()) {
         return false;
      } else if (random.nextBoolean()) {
         return false;
      } else if (level.getMaxLocalRawBrightness(pos) > random.nextInt(4)) {
         return false;
      } else {
         return !level.getBlockState(pos.below()).is(BlockTags.BATS_SPAWNABLE_ON) ? false : checkMobSpawnRules(type, level, spawnReason, pos, random);
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
