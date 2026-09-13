package net.minecraft.world.entity.animal.fish;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractFish extends WaterAnimal implements Bucketable {
   private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(AbstractFish.class, EntityDataSerializers.BOOLEAN);
   private static final boolean DEFAULT_FROM_BUCKET = false;

   public AbstractFish(final EntityType<? extends AbstractFish> type, final Level level) {
      super(type, level);
      this.moveControl = new AbstractFish.FishMoveControl<>(this);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0);
   }

   @Override
   public boolean requiresCustomPersistence() {
      return super.requiresCustomPersistence() || this.fromBucket();
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return !this.fromBucket() && !this.hasCustomName();
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 8;
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(FROM_BUCKET, false);
   }

   @Override
   public boolean fromBucket() {
      return this.entityData.get(FROM_BUCKET);
   }

   @Override
   public void setFromBucket(final boolean fromBucket) {
      this.entityData.set(FROM_BUCKET, fromBucket);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("FromBucket", this.fromBucket());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setFromBucket(input.getBooleanOr("FromBucket", false));
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new PanicGoal(this, 1.25));
      this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 1.6, 1.4, EntitySelector.NO_SPECTATORS));
      this.goalSelector.addGoal(4, new AbstractFish.FishSwimGoal(this));
   }

   @Override
   protected PathNavigation createNavigation(final Level level) {
      return new WaterBoundPathNavigation(this, level);
   }

   @Override
   protected void travelInWater(final Vec3 input, final double baseGravity, final boolean isFalling, final double oldY) {
      this.moveRelative(0.01F, input);
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
      if (this.getTarget() == null) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
      }
   }

   @Override
   public void aiStep() {
      if (!this.isInWater() && this.onGround() && this.verticalCollision) {
         this.setDeltaMovement(
            this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.05F, 0.4F, (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F)
         );
         this.setOnGround(false);
         this.needsSync = true;
         this.makeSound(this.getFlopSound());
      }

      super.aiStep();
   }

   @Override
   protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
   }

   @Override
   public void saveToBucketTag(final ItemStack bucket) {
      Bucketable.saveDefaultDataToBucketTag(this, bucket);
   }

   @Override
   public void loadFromBucketTag(final CompoundTag tag) {
      Bucketable.loadDefaultDataFromBucketTag(this, tag);
   }

   @Override
   public SoundEvent getPickupSound() {
      return SoundEvents.BUCKET_FILL_FISH;
   }

   protected boolean canRandomSwim() {
      return true;
   }

   protected abstract SoundEvent getFlopSound();

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.FISH_SWIM;
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
   }

   private static class FishMoveControl<T extends AbstractFish> extends MoveControl<T> {
      public FishMoveControl(final T fish) {
         super(fish);
      }

      @Override
      public void tick() {
         if (this.mob.isEyeInFluid(FluidTags.WATER)) {
            this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, 0.005, 0.0));
         }

         if (this.operation == MoveControl.Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
            float targetSpeed = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            this.mob.setSpeed(Mth.lerp(0.125F, this.mob.getSpeed(), targetSpeed));
            double xd = this.wantedX - this.mob.getX();
            double yd = this.wantedY - this.mob.getY();
            double zd = this.wantedZ - this.mob.getZ();
            if (yd != 0.0) {
               double dd = Math.sqrt(xd * xd + yd * yd + zd * zd);
               this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, this.mob.getSpeed() * (yd / dd) * 0.1, 0.0));
            }

            if (xd != 0.0 || zd != 0.0) {
               float yRotD = (float)(Mth.atan2(zd, xd) * 180.0F / (float)Math.PI) - 90.0F;
               this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yRotD, 90.0F));
               this.mob.yBodyRot = this.mob.getYRot();
            }
         } else {
            this.mob.setSpeed(0.0F);
         }
      }
   }

   private static class FishSwimGoal extends RandomSwimmingGoal {
      private final AbstractFish fish;

      public FishSwimGoal(final AbstractFish fish) {
         super(fish, 1.0, 40);
         this.fish = fish;
      }

      @Override
      public boolean canUse() {
         return this.fish.canRandomSwim() && super.canUse();
      }
   }
}
