package net.minecraft.world.entity.animal.chicken;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Chicken extends Animal {
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.3F, 0.4F)
      .withEyeHeight(0.28125F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.375F, 0.0F));
   private static final EntityDataAccessor<Holder<ChickenVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(
      Chicken.class, EntityDataSerializers.CHICKEN_VARIANT
   );
   private static final EntityDataAccessor<Holder<ChickenSoundVariant>> DATA_SOUND_VARIANT_ID = SynchedEntityData.defineId(
      Chicken.class, EntityDataSerializers.CHICKEN_SOUND_VARIANT
   );
   private static final boolean DEFAULT_CHICKEN_JOCKEY = false;
   public float flap;
   public float flapSpeed;
   public float oFlapSpeed;
   public float oFlap;
   public float flapping = 1.0F;
   private float nextFlap = 1.0F;
   public int eggTime;
   public boolean isChickenJockey = false;

   public Chicken(final EntityType<? extends Chicken> type, final Level level) {
      super(type, level);
      this.eggTime = this.random.nextInt(6000) + 6000;
      this.setPathfindingMalus(PathType.WATER, 0.0F);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
      this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, i -> i.is(ItemTags.CHICKEN_FOOD), false));
      this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
   }

   private Holder<ChickenSoundVariant> getSoundVariant() {
      return this.entityData.get(DATA_SOUND_VARIANT_ID);
   }

   private void setSoundVariant(final Holder<ChickenSoundVariant> soundVariant) {
      this.entityData.set(DATA_SOUND_VARIANT_ID, soundVariant);
   }

   private ChickenSoundVariant.ChickenSoundSet getSoundSet() {
      return this.isBaby() ? this.getSoundVariant().value().babySounds() : this.getSoundVariant().value().adultSounds();
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      this.oFlap = this.flap;
      this.oFlapSpeed = this.flapSpeed;
      this.flapSpeed = this.flapSpeed + (this.onGround() ? -1.0F : 4.0F) * 0.3F;
      this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
      if (!this.onGround() && this.flapping < 1.0F) {
         this.flapping = 1.0F;
      }

      this.flapping *= 0.9F;
      Vec3 movement = this.getDeltaMovement();
      if (!this.onGround() && movement.y < 0.0) {
         this.setDeltaMovement(movement.multiply(1.0, 0.6, 1.0));
      }

      this.flap = this.flap + this.flapping * 2.0F;
      if (this.level() instanceof ServerLevel level && this.isAlive() && !this.isBaby() && !this.isChickenJockey() && --this.eggTime <= 0) {
         if (this.dropFromGiftLootTable(level, BuiltInLootTables.CHICKEN_LAY, this::spawnAtLocation)) {
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.gameEvent(GameEvent.ENTITY_PLACE);
         }

         this.eggTime = this.random.nextInt(6000) + 6000;
      }
   }

   @Override
   protected boolean isFlapping() {
      return this.flyDist > this.nextFlap;
   }

   @Override
   protected void onFlap() {
      this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.getSoundSet().ambientSound().value();
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return this.getSoundSet().hurtSound().value();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.getSoundSet().deathSound().value();
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(this.getSoundSet().stepSound().value(), 0.15F, 1.0F);
   }

   public @Nullable Chicken getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      Chicken baby = EntityTypes.CHICKEN.create(level, EntitySpawnReason.BREEDING);
      if (baby != null && partner instanceof Chicken partnerChicken) {
         baby.setVariant(this.random.nextBoolean() ? this.getVariant() : partnerChicken.getVariant());
      }

      return baby;
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.CHICKEN_VARIANT).ifPresent(this::setVariant);
      this.setSoundVariant(ChickenSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom()));
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.CHICKEN_FOOD);
   }

   @Override
   protected int getBaseExperienceReward(final ServerLevel level) {
      return this.isChickenJockey() ? 10 : super.getBaseExperienceReward(level);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      Registry<ChickenSoundVariant> chickenSoundVariants = this.registryAccess().lookupOrThrow(Registries.CHICKEN_SOUND_VARIANT);
      entityData.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), ChickenVariants.TEMPERATE));
      entityData.define(DATA_SOUND_VARIANT_ID, chickenSoundVariants.get(ChickenSoundVariants.CLASSIC).or(chickenSoundVariants::getAny).orElseThrow());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.isChickenJockey = input.getBooleanOr("IsChickenJockey", false);
      input.getInt("EggLayTime").ifPresent(time -> this.eggTime = time);
      VariantUtils.readVariant(input, Registries.CHICKEN_VARIANT).ifPresent(this::setVariant);
      input.<ResourceKey>read("sound_variant", ResourceKey.codec(Registries.CHICKEN_SOUND_VARIANT))
         .flatMap(soundVariant -> this.registryAccess().lookupOrThrow(Registries.CHICKEN_SOUND_VARIANT).get((ResourceKey<ChickenSoundVariant>)soundVariant))
         .ifPresent(this::setSoundVariant);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("IsChickenJockey", this.isChickenJockey);
      output.putInt("EggLayTime", this.eggTime);
      VariantUtils.writeVariant(output, this.getVariant());
      this.getSoundVariant()
         .unwrapKey()
         .ifPresent(soundVariant -> output.store("sound_variant", ResourceKey.codec(Registries.CHICKEN_SOUND_VARIANT), soundVariant));
   }

   public void setVariant(final Holder<ChickenVariant> variant) {
      this.entityData.set(DATA_VARIANT_ID, variant);
   }

   public Holder<ChickenVariant> getVariant() {
      return this.entityData.get(DATA_VARIANT_ID);
   }

   @Override
   public <T> @Nullable T get(final DataComponentType<? extends T> type) {
      if (type == DataComponents.CHICKEN_VARIANT) {
         return castComponentValue((DataComponentType<T>)type, this.getVariant());
      } else {
         return type == DataComponents.CHICKEN_SOUND_VARIANT ? castComponentValue((DataComponentType<T>)type, this.getSoundVariant()) : super.get(type);
      }
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      this.applyImplicitComponentIfPresent(components, DataComponents.CHICKEN_VARIANT);
      this.applyImplicitComponentIfPresent(components, DataComponents.CHICKEN_SOUND_VARIANT);
      super.applyImplicitComponents(components);
   }

   @Override
   protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
      if (type == DataComponents.CHICKEN_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.CHICKEN_VARIANT, value));
         return true;
      } else if (type == DataComponents.CHICKEN_SOUND_VARIANT) {
         this.setSoundVariant(castComponentValue(DataComponents.CHICKEN_SOUND_VARIANT, value));
         return true;
      } else {
         return super.applyImplicitComponent(type, value);
      }
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return this.isChickenJockey();
   }

   @Override
   protected void positionRider(final Entity passenger, final Entity.MoveFunction moveFunction) {
      super.positionRider(passenger, moveFunction);
      if (passenger instanceof LivingEntity livingEntity) {
         livingEntity.yBodyRot = this.yBodyRot;
      }
   }

   public boolean isChickenJockey() {
      return this.isChickenJockey;
   }

   public void setChickenJockey(final boolean isChickenJockey) {
      this.isChickenJockey = isChickenJockey;
   }
}
