package net.minecraft.world.entity.animal.frog;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Tadpole extends AbstractFish {
   private static final int DEFAULT_AGE = 0;
   private static final EntityDataAccessor<Boolean> AGE_LOCKED = SynchedEntityData.defineId(Tadpole.class, EntityDataSerializers.BOOLEAN);
   @VisibleForTesting
   public static int ticksToBeFrog = Math.abs(-24000);
   public static final float HITBOX_WIDTH = 0.4F;
   public static final float HITBOX_HEIGHT = 0.3F;
   private int age = 0;
   protected int ageLockParticleTimer = 0;
   private static final Brain.Provider<Tadpole> BRAIN_PROVIDER = Brain.provider(
      List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY, SensorType.FROG_TEMPTATIONS),
      var0 -> TadpoleAi.getActivities()
   );

   public Tadpole(final EntityType<? extends AbstractFish> type, final Level level) {
      super(type, level);
      this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.02F, 0.1F, true);
      this.lookControl = new SmoothSwimmingLookControl(this, 10);
   }

   @Override
   protected PathNavigation createNavigation(final Level level) {
      return new WaterBoundPathNavigation(this, level);
   }

   @Override
   protected Brain<Tadpole> makeBrain(final Brain.Packed packedBrain) {
      return BRAIN_PROVIDER.makeBrain(this, packedBrain);
   }

   @Override
   public Brain<Tadpole> getBrain() {
      return super.getBrain();
   }

   @Override
   protected SoundEvent getFlopSound() {
      return SoundEvents.TADPOLE_FLOP;
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("tadpoleBrain");
      this.getBrain().tick(level, this);
      profiler.pop();
      profiler.push("tadpoleActivityUpdate");
      TadpoleAi.updateActivity(this);
      profiler.pop();
      super.customServerAiStep(level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 1.0).add(Attributes.MAX_HEALTH, 6.0);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide() && !this.isAgeLocked()) {
         this.setAge(this.age + 1);
      }

      this.ageLockParticleTimer = AgeableMob.makeAgeLockedParticle(this.level(), this, this.ageLockParticleTimer, this.isAgeLocked());
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("Age", this.age);
      output.putBoolean("AgeLocked", this.isAgeLocked());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setAge(input.getIntOr("Age", 0));
      this.setAgeLocked(input.getBooleanOr("AgeLocked", false));
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(AGE_LOCKED, false);
   }

   protected void setAgeLocked(final boolean locked) {
      this.entityData.set(AGE_LOCKED, locked);
   }

   public boolean isAgeLocked() {
      return this.entityData.get(AGE_LOCKED);
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      return null;
   }

   @Override
   protected @Nullable SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.TADPOLE_HURT;
   }

   @Override
   protected @Nullable SoundEvent getDeathSound() {
      return SoundEvents.TADPOLE_DEATH;
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (this.isFood(itemStack) && !this.isAgeLocked()) {
         this.feed(player, itemStack);
         return InteractionResult.SUCCESS;
      } else if (AgeableMob.canUseGoldenDandelion(itemStack, true, this.ageLockParticleTimer, this)) {
         AgeableMob.setAgeLocked(this, this::isAgeLocked, player, itemStack, mob -> this.setAgeLockedData());
         return InteractionResult.SUCCESS;
      } else {
         return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
      }
   }

   private void setAgeLockedData() {
      this.setAgeLocked(!this.isAgeLocked());
      this.setAge(0);
      this.ageLockParticleTimer = 40;
   }

   @Override
   public boolean fromBucket() {
      return true;
   }

   @Override
   public void setFromBucket(final boolean fromBucket) {
   }

   @Override
   public void saveToBucketTag(final ItemStack bucket) {
      Bucketable.saveDefaultDataToBucketTag(this, bucket);
      CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
         tag.putInt("Age", this.getAge());
         tag.putBoolean("AgeLocked", this.isAgeLocked());
      });
   }

   @Override
   public void loadFromBucketTag(final CompoundTag tag) {
      Bucketable.loadDefaultDataFromBucketTag(this, tag);
      tag.getInt("Age").ifPresent(this::setAge);
      this.setAgeLocked(tag.getBooleanOr("AgeLocked", false));
   }

   @Override
   public ItemStack getBucketItemStack() {
      return new ItemStack(Items.TADPOLE_BUCKET);
   }

   @Override
   public SoundEvent getPickupSound() {
      return SoundEvents.BUCKET_FILL_TADPOLE;
   }

   private boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.FROG_FOOD);
   }

   private void feed(final Player player, final ItemStack itemStack) {
      this.usePlayerItem(player, itemStack);
      this.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(this.getTicksLeftUntilAdult()));
      this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
   }

   private void usePlayerItem(final Player player, final ItemStack itemStack) {
      itemStack.consume(1, player);
   }

   private int getAge() {
      return this.age;
   }

   private void ageUp(final int ticksToAgeUp) {
      this.setAge(this.age + ticksToAgeUp * 20);
   }

   private void setAge(final int newAge) {
      this.age = newAge;
      if (this.age >= ticksToBeFrog) {
         this.ageUp();
      }
   }

   private void ageUp() {
      if (this.level() instanceof ServerLevel serverLevel) {
         this.convertTo(EntityTypes.FROG, ConversionParams.single(this, false, false), frog -> {
            frog.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(frog.blockPosition()), EntitySpawnReason.CONVERSION, null);
            frog.setPersistenceRequired();
            frog.fudgePositionAfterSizeChange(this.getDimensions(this.getPose()));
            this.playSound(SoundEvents.TADPOLE_GROW_UP, 0.15F, 1.0F);
         });
      }
   }

   private int getTicksLeftUntilAdult() {
      return Math.max(0, ticksToBeFrog - this.age);
   }

   @Override
   public boolean shouldDropExperience() {
      return false;
   }
}
