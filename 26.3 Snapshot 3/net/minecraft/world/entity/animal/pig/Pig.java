package net.minecraft.world.entity.animal.pig;

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
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LightningBolt;
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
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Pig extends Animal implements ItemSteerable {
   private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Holder<PigVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.PIG_VARIANT);
   private static final EntityDataAccessor<Holder<PigSoundVariant>> DATA_SOUND_VARIANT_ID = SynchedEntityData.defineId(
      Pig.class, EntityDataSerializers.PIG_SOUND_VARIANT
   );
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.45F, 0.45F)
      .withEyeHeight(0.40625F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.5F, 0.0F));
   private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);

   public Pig(final EntityType<? extends Pig> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
      this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, i -> i.is(Items.CARROT_ON_A_STICK), false));
      this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, i -> i.is(ItemTags.PIG_FOOD), false));
      this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      return this.isSaddled() && this.getFirstPassenger() instanceof Player player && player.isHolding(Items.CARROT_ON_A_STICK)
         ? player
         : super.getControllingPassenger();
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      if (DATA_BOOST_TIME.equals(accessor) && this.level().isClientSide()) {
         this.steering.onSynced();
      }

      super.onSyncedDataUpdated(accessor);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      Registry<PigSoundVariant> pigSoundVariants = this.registryAccess().lookupOrThrow(Registries.PIG_SOUND_VARIANT);
      entityData.define(DATA_BOOST_TIME, 0);
      entityData.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), PigVariants.DEFAULT));
      entityData.define(DATA_SOUND_VARIANT_ID, pigSoundVariants.get(PigSoundVariants.CLASSIC).or(pigSoundVariants::getAny).orElseThrow());
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      VariantUtils.writeVariant(output, this.getVariant());
      this.getSoundVariant()
         .unwrapKey()
         .ifPresent(soundVariant -> output.store("sound_variant", ResourceKey.codec(Registries.PIG_SOUND_VARIANT), soundVariant));
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      VariantUtils.readVariant(input, Registries.PIG_VARIANT).ifPresent(this::setVariant);
      input.<ResourceKey>read("sound_variant", ResourceKey.codec(Registries.PIG_SOUND_VARIANT))
         .flatMap(soundVariant -> this.registryAccess().lookupOrThrow(Registries.PIG_SOUND_VARIANT).get((ResourceKey<PigSoundVariant>)soundVariant))
         .ifPresent(this::setSoundVariant);
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
   protected void playEatingSound() {
      this.makeSound(this.getSoundSet().eatSound().value());
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(this.getSoundSet().stepSound().value(), 0.15F, 1.0F);
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      boolean hasFood = this.isFood(player.getItemInHand(hand));
      if (!hasFood && this.isSaddled() && !this.isVehicle() && !player.isSecondaryUseActive()) {
         if (!this.level().isClientSide()) {
            player.startRiding(this);
         }

         return InteractionResult.SUCCESS;
      } else {
         InteractionResult interactionResult = super.mobInteract(player, hand);
         if (!interactionResult.consumesAction()) {
            ItemStack itemStack = player.getItemInHand(hand);
            return this.isEquippableInSlot(itemStack, EquipmentSlot.SADDLE) ? itemStack.interactLivingEntity(player, this, hand) : InteractionResult.PASS;
         } else {
            return interactionResult;
         }
      }
   }

   @Override
   public boolean canUseSlot(final EquipmentSlot slot) {
      return slot != EquipmentSlot.SADDLE ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby();
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
      return slot == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(slot);
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(final EquipmentSlot slot, final ItemStack stack, final Equippable equippable) {
      return slot == EquipmentSlot.SADDLE ? SoundEvents.PIG_SADDLE : super.getEquipSound(slot, stack, equippable);
   }

   @Override
   public void thunderHit(final ServerLevel level, final LightningBolt lightningBolt) {
      if (level.getDifficulty() != Difficulty.PEACEFUL) {
         ZombifiedPiglin zombifiedPiglin = this.convertTo(EntityTypes.ZOMBIFIED_PIGLIN, ConversionParams.single(this, false, true), zp -> {
            zp.populateDefaultEquipmentSlots(this.getRandom(), level.getCurrentDifficultyAt(this.blockPosition()));
            zp.setPersistenceRequired();
         });
         if (zombifiedPiglin == null) {
            super.thunderHit(level, lightningBolt);
         }
      } else {
         super.thunderHit(level, lightningBolt);
      }
   }

   @Override
   protected void tickRidden(final Player controller, final Vec3 riddenInput) {
      super.tickRidden(controller, riddenInput);
      this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
      this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
      this.steering.tickBoost();
   }

   @Override
   protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
      return new Vec3(0.0, 0.0, 1.0);
   }

   @Override
   protected float getRiddenSpeed(final Player controller) {
      return (float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225 * this.steering.boostFactor());
   }

   @Override
   public boolean boost() {
      return this.steering.boost(this.getRandom());
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   public @Nullable Pig getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      Pig baby = EntityTypes.PIG.create(level, EntitySpawnReason.BREEDING);
      if (baby != null && partner instanceof Pig partnerPig) {
         baby.setVariant(this.random.nextBoolean() ? this.getVariant() : partnerPig.getVariant());
      }

      return baby;
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.PIG_FOOD);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   private void setVariant(final Holder<PigVariant> variant) {
      this.entityData.set(DATA_VARIANT_ID, variant);
   }

   public Holder<PigVariant> getVariant() {
      return this.entityData.get(DATA_VARIANT_ID);
   }

   private Holder<PigSoundVariant> getSoundVariant() {
      return this.entityData.get(DATA_SOUND_VARIANT_ID);
   }

   private void setSoundVariant(final Holder<PigSoundVariant> soundVariant) {
      this.entityData.set(DATA_SOUND_VARIANT_ID, soundVariant);
   }

   private PigSoundVariant.PigSoundSet getSoundSet() {
      return this.isBaby() ? this.getSoundVariant().value().babySounds() : this.getSoundVariant().value().adultSounds();
   }

   @Override
   public <T> @Nullable T get(final DataComponentType<? extends T> type) {
      if (type == DataComponents.PIG_VARIANT) {
         return castComponentValue((DataComponentType<T>)type, this.getVariant());
      } else {
         return type == DataComponents.PIG_SOUND_VARIANT ? castComponentValue((DataComponentType<T>)type, this.getSoundVariant()) : super.get(type);
      }
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      this.applyImplicitComponentIfPresent(components, DataComponents.PIG_VARIANT);
      this.applyImplicitComponentIfPresent(components, DataComponents.PIG_SOUND_VARIANT);
      super.applyImplicitComponents(components);
   }

   @Override
   protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
      if (type == DataComponents.PIG_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.PIG_VARIANT, value));
         return true;
      } else if (type == DataComponents.PIG_SOUND_VARIANT) {
         this.setSoundVariant(castComponentValue(DataComponents.PIG_SOUND_VARIANT, value));
         return true;
      } else {
         return super.applyImplicitComponent(type, value);
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.PIG_VARIANT).ifPresent(this::setVariant);
      this.setSoundVariant(PigSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom()));
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }
}
