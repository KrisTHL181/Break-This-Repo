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
import net.minecraft.world.entity.animal.Bucketable;
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

   public AbstractFish(EntityType<? extends AbstractFish> p_453888_, Level p_456907_) {
      super(p_453888_, p_456907_);
      this.moveControl = new AbstractFish.FishMoveControl(this);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0);
   }

   @Override
   public boolean requiresCustomPersistence() {
      return super.requiresCustomPersistence() || this.fromBucket();
   }

   @Override
   public boolean removeWhenFarAway(double p_452414_) {
      return !this.fromBucket() && !this.hasCustomName();
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 8;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454521_) {
      super.defineSynchedData(p_454521_);
      p_454521_.define(FROM_BUCKET, false);
   }

   @Override
   public boolean fromBucket() {
      return this.entityData.get(FROM_BUCKET);
   }

   @Override
   public void setFromBucket(boolean p_451330_) {
      this.entityData.set(FROM_BUCKET, p_451330_);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_454495_) {
      super.addAdditionalSaveData(p_454495_);
      p_454495_.putBoolean("FromBucket", this.fromBucket());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_454301_) {
      super.readAdditionalSaveData(p_454301_);
      this.setFromBucket(p_454301_.getBooleanOr("FromBucket", false));
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new PanicGoal(this, 1.25));
      this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 1.6, 1.4, EntitySelector.NO_SPECTATORS));
      this.goalSelector.addGoal(4, new AbstractFish.FishSwimGoal(this));
   }

   @Override
   protected PathNavigation createNavigation(Level p_453596_) {
      return new WaterBoundPathNavigation(this, p_453596_);
   }

   @Override
   protected void travelInWater(Vec3 p_452916_, double p_459747_, boolean p_456797_, double p_456353_) {
      this.moveRelative(0.01F, p_452916_);
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
   protected InteractionResult mobInteract(Player p_457766_, InteractionHand p_452755_) {
      return Bucketable.bucketMobPickup(p_457766_, p_452755_, this).orElse(super.mobInteract(p_457766_, p_452755_));
   }

   @Override
   public void saveToBucketTag(ItemStack p_451727_) {
      Bucketable.saveDefaultDataToBucketTag(this, p_451727_);
   }

   @Override
   public void loadFromBucketTag(CompoundTag p_460191_) {
      Bucketable.loadDefaultDataFromBucketTag(this, p_460191_);
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
   protected void playStepSound(BlockPos p_454734_, BlockState p_456844_) {
   }

   static class FishMoveControl extends MoveControl {
      private final AbstractFish fish;

      FishMoveControl(AbstractFish p_459833_) {
         super(p_459833_);
         this.fish = p_459833_;
      }

      @Override
      public void tick() {
         if (this.fish.isEyeInFluid(FluidTags.WATER)) {
            this.fish.setDeltaMovement(this.fish.getDeltaMovement().add(0.0, 0.005, 0.0));
         }

         if (this.operation == MoveControl.Operation.MOVE_TO && !this.fish.getNavigation().isDone()) {
            float f = (float)(this.speedModifier * this.fish.getAttributeValue(Attributes.MOVEMENT_SPEED));
            this.fish.setSpeed(Mth.lerp(0.125F, this.fish.getSpeed(), f));
            double d0 = this.wantedX - this.fish.getX();
            double d1 = this.wantedY - this.fish.getY();
            double d2 = this.wantedZ - this.fish.getZ();
            if (d1 != 0.0) {
               double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
               this.fish.setDeltaMovement(this.fish.getDeltaMovement().add(0.0, this.fish.getSpeed() * (d1 / d3) * 0.1, 0.0));
            }

            if (d0 != 0.0 || d2 != 0.0) {
               float f1 = (float)(Mth.atan2(d2, d0) * 180.0 / (float) Math.PI) - 90.0F;
               this.fish.setYRot(this.rotlerp(this.fish.getYRot(), f1, 90.0F));
               this.fish.yBodyRot = this.fish.getYRot();
            }
         } else {
            this.fish.setSpeed(0.0F);
         }
      }
   }

   static class FishSwimGoal extends RandomSwimmingGoal {
      private final AbstractFish fish;

      public FishSwimGoal(AbstractFish p_459723_) {
         super(p_459723_, 1.0, 40);
         this.fish = p_459723_;
      }

      @Override
      public boolean canUse() {
         return this.fish.canRandomSwim() && super.canUse();
      }
   }
}
