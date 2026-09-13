package net.minecraft.world.entity.animal.wolf;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BegGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Wolf extends TamableAnimal implements NeutralMob {
   private static final EntityDataAccessor<Boolean> DATA_INTERESTED_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Long> DATA_ANGER_END_TIME = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.LONG);
   private static final EntityDataAccessor<Holder<WolfVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.WOLF_VARIANT);
   private static final EntityDataAccessor<Holder<WolfSoundVariant>> DATA_SOUND_VARIANT_ID = SynchedEntityData.defineId(
      Wolf.class, EntityDataSerializers.WOLF_SOUND_VARIANT
   );
   public static final TargetingConditions.Selector PREY_SELECTOR = (target, level) -> target.is(EntityTypes.SHEEP)
      || target.is(EntityTypes.RABBIT)
      || target.is(EntityTypes.FOX);
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.3F, 0.425F)
      .withEyeHeight(0.34375F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.4375F, 0.0F));
   private static final float START_HEALTH = 8.0F;
   private static final float TAME_HEALTH = 40.0F;
   private static final float ARMOR_REPAIR_UNIT = 0.125F;
   public static final float DEFAULT_TAIL_ANGLE = (float) (Math.PI / 5);
   private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.RED;
   private float interestedAngle;
   private float interestedAngleO;
   private boolean isWet;
   private boolean isShaking;
   private float shakeAnim;
   private float shakeAnimO;
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

   public Wolf(final EntityType<? extends Wolf> type, final Level level) {
      super(type, level);
      this.setTame(false, false);
      this.setPathfindingMalus(PathType.POWDER_SNOW, -1.0F);
      this.setPathfindingMalus(PathType.ON_TOP_OF_POWDER_SNOW, -1.0F);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
      this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(3, new Wolf.WolfAvoidEntityGoal<>(this, Llama.class, 24.0F, 1.5, 1.5));
      this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
      this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
      this.goalSelector.addGoal(7, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(9, new BegGoal(this, 8.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
      this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
      this.targetSelector.addGoal(5, new NonTameRandomTargetGoal<>(this, Animal.class, false, PREY_SELECTOR));
      this.targetSelector.addGoal(6, new NonTameRandomTargetGoal<>(this, Turtle.class, false, Turtle.BABY_ON_LAND_SELECTOR));
      this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, false));
      this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal<>(this, true));
   }

   public Identifier getTexture() {
      WolfVariant variant = this.getVariant().value();
      WolfVariant.AssetInfo assetInfo = this.isBaby() ? variant.babyInfo() : variant.adultInfo();
      if (this.isTame()) {
         return assetInfo.tame().texturePath();
      } else {
         return this.isAngry() ? assetInfo.angry().texturePath() : assetInfo.wild().texturePath();
      }
   }

   private Holder<WolfVariant> getVariant() {
      return this.entityData.get(DATA_VARIANT_ID);
   }

   private void setVariant(final Holder<WolfVariant> variant) {
      this.entityData.set(DATA_VARIANT_ID, variant);
   }

   private Holder<WolfSoundVariant> getSoundVariant() {
      return this.entityData.get(DATA_SOUND_VARIANT_ID);
   }

   private WolfSoundVariant.WolfSoundSet getSoundSet() {
      return this.isBaby() ? this.getSoundVariant().value().babySounds() : this.getSoundVariant().value().adultSounds();
   }

   private void setSoundVariant(final Holder<WolfSoundVariant> soundVariant) {
      this.entityData.set(DATA_SOUND_VARIANT_ID, soundVariant);
   }

   @Override
   public <T> @Nullable T get(final DataComponentType<? extends T> type) {
      if (type == DataComponents.WOLF_VARIANT) {
         return castComponentValue((DataComponentType<T>)type, this.getVariant());
      } else if (type == DataComponents.WOLF_SOUND_VARIANT) {
         return castComponentValue((DataComponentType<T>)type, this.getSoundVariant());
      } else {
         return type == DataComponents.WOLF_COLLAR ? castComponentValue((DataComponentType<T>)type, this.getCollarColor()) : super.get(type);
      }
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      this.applyImplicitComponentIfPresent(components, DataComponents.WOLF_VARIANT);
      this.applyImplicitComponentIfPresent(components, DataComponents.WOLF_SOUND_VARIANT);
      this.applyImplicitComponentIfPresent(components, DataComponents.WOLF_COLLAR);
      super.applyImplicitComponents(components);
   }

   @Override
   protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
      if (type == DataComponents.WOLF_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.WOLF_VARIANT, value));
         return true;
      } else if (type == DataComponents.WOLF_SOUND_VARIANT) {
         this.setSoundVariant(castComponentValue(DataComponents.WOLF_SOUND_VARIANT, value));
         return true;
      } else if (type == DataComponents.WOLF_COLLAR) {
         this.setCollarColor(castComponentValue(DataComponents.WOLF_COLLAR, value));
         return true;
      } else {
         return super.applyImplicitComponent(type, value);
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 4.0);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      Registry<WolfSoundVariant> wolfSoundVariants = this.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT);
      entityData.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), WolfVariants.DEFAULT));
      entityData.define(DATA_SOUND_VARIANT_ID, wolfSoundVariants.get(WolfSoundVariants.CLASSIC).or(wolfSoundVariants::getAny).orElseThrow());
      entityData.define(DATA_INTERESTED_ID, false);
      entityData.define(DATA_COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getId());
      entityData.define(DATA_ANGER_END_TIME, -1L);
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(this.getSoundSet().stepSound().value(), 0.15F, 1.0F);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.store("CollarColor", DyeColor.LEGACY_ID_CODEC, this.getCollarColor());
      VariantUtils.writeVariant(output, this.getVariant());
      this.addPersistentAngerSaveData(output);
      this.getSoundVariant()
         .unwrapKey()
         .ifPresent(soundVariant -> output.store("sound_variant", ResourceKey.codec(Registries.WOLF_SOUND_VARIANT), soundVariant));
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      VariantUtils.readVariant(input, Registries.WOLF_VARIANT).ifPresent(this::setVariant);
      this.setCollarColor(input.<DyeColor>read("CollarColor", DyeColor.LEGACY_ID_CODEC).orElse(DEFAULT_COLLAR_COLOR));
      this.readPersistentAngerSaveData(this.level(), input);
      input.<ResourceKey>read("sound_variant", ResourceKey.codec(Registries.WOLF_SOUND_VARIANT))
         .flatMap(soundVariant -> this.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT).get((ResourceKey<WolfSoundVariant>)soundVariant))
         .ifPresent(this::setSoundVariant);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      if (groupData instanceof Wolf.WolfPackData wolfGroupData) {
         this.setVariant(wolfGroupData.type);
      } else {
         Optional<? extends Holder<WolfVariant>> selectedVariant = VariantUtils.selectVariantToSpawn(
            SpawnContext.create(level, this.blockPosition()), Registries.WOLF_VARIANT
         );
         if (selectedVariant.isPresent()) {
            this.setVariant((Holder<WolfVariant>)selectedVariant.get());
            groupData = new Wolf.WolfPackData((Holder<WolfVariant>)selectedVariant.get());
         }
      }

      this.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom()));
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      if (this.isAngry()) {
         return this.getSoundSet().growlSound().value();
      } else if (this.random.nextInt(3) == 0) {
         return this.isTame() && this.getHealth() < 20.0F ? this.getSoundSet().whineSound().value() : this.getSoundSet().pantSound().value();
      } else {
         return this.getSoundSet().ambientSound().value();
      }
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return this.canArmorAbsorb(source) ? SoundEvents.WOLF_ARMOR_DAMAGE : this.getSoundSet().hurtSound().value();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.getSoundSet().deathSound().value();
   }

   @Override
   protected float getSoundVolume() {
      return 0.4F;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide() && this.isWet && !this.isShaking && !this.isPathFinding() && this.onGround()) {
         this.isShaking = true;
         this.shakeAnim = 0.0F;
         this.shakeAnimO = 0.0F;
         this.level().broadcastEntityEvent(this, (byte)8);
      }

      if (!this.level().isClientSide()) {
         this.updatePersistentAnger((ServerLevel)this.level(), true);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isAlive()) {
         this.interestedAngleO = this.interestedAngle;
         if (this.isInterested()) {
            this.interestedAngle = this.interestedAngle + (1.0F - this.interestedAngle) * 0.4F;
         } else {
            this.interestedAngle = this.interestedAngle + (0.0F - this.interestedAngle) * 0.4F;
         }

         if (this.isInWaterOrRain()) {
            this.isWet = true;
            if (this.isShaking && !this.level().isClientSide()) {
               this.level().broadcastEntityEvent(this, (byte)56);
               this.cancelShake();
            }
         } else if ((this.isWet || this.isShaking) && this.isShaking) {
            if (this.shakeAnim == 0.0F) {
               this.playSound(SoundEvents.WOLF_SHAKE, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
               this.gameEvent(GameEvent.ENTITY_ACTION);
            }

            this.shakeAnimO = this.shakeAnim;
            this.shakeAnim += 0.05F;
            if (this.shakeAnimO >= 2.0F) {
               this.isWet = false;
               this.isShaking = false;
               this.shakeAnimO = 0.0F;
               this.shakeAnim = 0.0F;
            }

            if (this.shakeAnim > 0.4F) {
               float yt = (float)this.getY();
               int shakeCount = (int)(Mth.sin((this.shakeAnim - 0.4F) * (float) Math.PI) * 7.0F);
               Vec3 movement = this.getDeltaMovement();

               for (int i = 0; i < shakeCount; i++) {
                  float xo = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;
                  float zo = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;
                  this.level().addParticle(ParticleTypes.SPLASH, this.getX() + xo, yt + 0.8F, this.getZ() + zo, movement.x, movement.y, movement.z);
               }
            }
         }
      }
   }

   private void cancelShake() {
      this.isShaking = false;
      this.shakeAnim = 0.0F;
      this.shakeAnimO = 0.0F;
   }

   @Override
   public void die(final DamageSource source) {
      this.isWet = false;
      this.isShaking = false;
      this.shakeAnimO = 0.0F;
      this.shakeAnim = 0.0F;
      super.die(source);
   }

   public float getWetShade(final float a) {
      return !this.isWet ? 1.0F : Math.min(0.75F + Mth.lerp(a, this.shakeAnimO, this.shakeAnim) / 2.0F * 0.25F, 1.0F);
   }

   public float getShakeAnim(final float a) {
      return Mth.lerp(a, this.shakeAnimO, this.shakeAnim);
   }

   public float getHeadRollAngle(final float a) {
      return Mth.lerp(a, this.interestedAngleO, this.interestedAngle) * 0.15F * (float) Math.PI;
   }

   @Override
   public int getMaxHeadXRot() {
      return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      if (this.isInvulnerableTo(level, source)) {
         return false;
      }

      this.setOrderedToSit(false);
      return super.hurtServer(level, source, damage);
   }

   @Override
   protected void actuallyHurt(final ServerLevel level, final DamageSource source, final float damage) {
      if (!this.canArmorAbsorb(source)) {
         super.actuallyHurt(level, source, damage);
      } else {
         ItemStack armorBefore = this.getBodyArmorItem();
         int damageBefore = armorBefore.getDamageValue();
         int maxDamage = armorBefore.getMaxDamage();
         armorBefore.hurtAndBreak(Mth.ceil(damage), this, EquipmentSlot.BODY);
         if (Crackiness.WOLF_ARMOR.byDamage(damageBefore, maxDamage) != Crackiness.WOLF_ARMOR.byDamage(this.getBodyArmorItem())) {
            this.playSound(SoundEvents.WOLF_ARMOR_CRACK);
            level.sendParticles(
               new ItemParticleOption(ParticleTypes.ITEM, Items.ARMADILLO_SCUTE), this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.2, 0.1, 0.2, 0.1
            );
         }
      }
   }

   private boolean canArmorAbsorb(final DamageSource source) {
      return this.getBodyArmorItem().is(Items.WOLF_ARMOR) && !source.is(DamageTypeTags.BYPASSES_WOLF_ARMOR);
   }

   @Override
   protected void applyTamingSideEffects() {
      if (this.isTame()) {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
         this.setHealth(40.0F);
      } else {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0);
      }
   }

   @Override
   protected void hurtArmor(final DamageSource damageSource, final float damage) {
      this.doHurtEquipment(damageSource, damage, EquipmentSlot.BODY);
   }

   @Override
   protected boolean canShearEquipment(final Player player) {
      return this.isOwnedBy(player);
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (this.isTame()) {
         if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
            this.feed(player, hand, itemStack, 2.0F, 2.0F);
            return InteractionResult.SUCCESS;
         }

         if (!itemStack.is(ItemTags.WOLF_COLLAR_DYES) || !this.isOwnedBy(player)) {
            if (this.isEquippableInSlot(itemStack, EquipmentSlot.BODY) && !this.isWearingBodyArmor() && this.isOwnedBy(player) && !this.isBaby()) {
               this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, itemStack.copyWithCount(1));
               itemStack.consume(1, player);
               return InteractionResult.SUCCESS;
            }

            if (this.isInSittingPose()
               && this.isWearingBodyArmor()
               && this.isOwnedBy(player)
               && this.getBodyArmorItem().isDamaged()
               && this.getBodyArmorItem().isValidRepairItem(itemStack)) {
               itemStack.shrink(1);
               this.playSound(SoundEvents.WOLF_ARMOR_REPAIR);
               ItemStack armor = this.getBodyArmorItem();
               int repairUnit = (int)(armor.getMaxDamage() * 0.125F);
               armor.setDamageValue(Math.max(0, armor.getDamageValue() - repairUnit));
               return InteractionResult.SUCCESS;
            }

            InteractionResult interactionResult = super.mobInteract(player, hand);
            if (!interactionResult.consumesAction() && this.isOwnedBy(player)) {
               this.setOrderedToSit(!this.isOrderedToSit());
               this.jumping = false;
               this.navigation.stop();
               this.setTarget(null);
               return InteractionResult.SUCCESS.withoutItem();
            }

            return interactionResult;
         }

         DyeColor color = itemStack.get(DataComponents.DYE);
         if (color != null && color != this.getCollarColor()) {
            this.setCollarColor(color);
            itemStack.consume(1, player);
            return InteractionResult.SUCCESS;
         }
      } else if (!this.level().isClientSide() && itemStack.is(Items.BONE) && !this.isAngry()) {
         itemStack.consume(1, player);
         this.tryToTame(player);
         return InteractionResult.SUCCESS_SERVER;
      }

      return super.mobInteract(player, hand);
   }

   private void tryToTame(final Player player) {
      if (this.random.nextInt(3) == 0) {
         this.tame(player);
         this.navigation.stop();
         this.setTarget(null);
         this.setOrderedToSit(true);
         this.level().broadcastEntityEvent(this, (byte)7);
      } else {
         this.level().broadcastEntityEvent(this, (byte)6);
      }
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 8) {
         this.isShaking = true;
         this.shakeAnim = 0.0F;
         this.shakeAnimO = 0.0F;
      } else if (id == 56) {
         this.cancelShake();
      } else {
         super.handleEntityEvent(id);
      }
   }

   public float getTailAngle() {
      if (this.isAngry()) {
         return 1.5393804F;
      } else if (this.isTame()) {
         float maxHealth = this.getMaxHealth();
         float damageRatio = (maxHealth - this.getHealth()) / maxHealth;
         return (0.55F - damageRatio * 0.4F) * (float) Math.PI;
      } else {
         return (float) (Math.PI / 5);
      }
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.WOLF_FOOD);
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 8;
   }

   @Override
   public long getPersistentAngerEndTime() {
      return this.entityData.get(DATA_ANGER_END_TIME);
   }

   @Override
   public void setPersistentAngerEndTime(final long endTime) {
      this.entityData.set(DATA_ANGER_END_TIME, endTime);
   }

   @Override
   public void startPersistentAngerTimer() {
      this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   @Override
   public void setPersistentAngerTarget(final @Nullable EntityReference<LivingEntity> persistentAngerTarget) {
      this.persistentAngerTarget = persistentAngerTarget;
   }

   public DyeColor getCollarColor() {
      return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
   }

   private void setCollarColor(final DyeColor color) {
      this.entityData.set(DATA_COLLAR_COLOR, color.getId());
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   public @Nullable Wolf getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      Wolf baby = EntityTypes.WOLF.create(level, EntitySpawnReason.BREEDING);
      if (baby != null && partner instanceof Wolf partnerWolf) {
         if (this.random.nextBoolean()) {
            baby.setVariant(this.getVariant());
         } else {
            baby.setVariant(partnerWolf.getVariant());
         }

         if (this.isTame()) {
            baby.setOwnerReference(this.getOwnerReference());
            baby.setTame(true, true);
            DyeColor parent1CollarColor = this.getCollarColor();
            DyeColor parent2CollarColor = partnerWolf.getCollarColor();
            baby.setCollarColor(DyeColor.getMixedColor(level, parent1CollarColor, parent2CollarColor));
         }

         baby.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), this.random));
      }

      return baby;
   }

   public void setIsInterested(final boolean value) {
      this.entityData.set(DATA_INTERESTED_ID, value);
   }

   @Override
   public boolean canMate(final Animal partner) {
      if (partner == this) {
         return false;
      } else if (!this.isTame()) {
         return false;
      } else if (!(partner instanceof Wolf wolf)) {
         return false;
      } else if (!wolf.isTame()) {
         return false;
      } else {
         return wolf.isInSittingPose() ? false : this.isInLove() && wolf.isInLove();
      }
   }

   public boolean isInterested() {
      return this.entityData.get(DATA_INTERESTED_ID);
   }

   @Override
   public boolean wantsToAttack(final LivingEntity target, final LivingEntity owner) {
      if (target instanceof Creeper || target instanceof Ghast || target instanceof ArmorStand) {
         return false;
      } else if (target instanceof Wolf wolfTarget) {
         return !wolfTarget.isTame() || wolfTarget.getOwner() != owner;
      } else if (target instanceof Player playerTarget && owner instanceof Player playerOwner && !playerOwner.canHarmPlayer(playerTarget)) {
         return false;
      } else {
         return target instanceof AbstractHorse horse && horse.isTamed() ? false : !(target instanceof TamableAnimal animal && animal.isTame());
      }
   }

   @Override
   public boolean canBeLeashed() {
      return !this.isAngry();
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   public static boolean checkWolfSpawnRules(
      final EntityType<Wolf> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return level.getBlockState(pos.below()).is(BlockTags.WOLVES_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
   }

   private class WolfAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
      private final Wolf wolf;

      public WolfAvoidEntityGoal(
         final Wolf wolf, final Class<T> avoidClass, final float maxDist, final double walkSpeedModifier, final double sprintSpeedModifier
      ) {
         super(wolf, avoidClass, maxDist, walkSpeedModifier, sprintSpeedModifier);
         this.wolf = wolf;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && this.toAvoid instanceof Llama ? !this.wolf.isTame() && this.avoidLlama((Llama)this.toAvoid) : false;
      }

      private boolean avoidLlama(final Llama llama) {
         return llama.getStrength() >= Wolf.this.random.nextInt(5);
      }

      @Override
      public void start() {
         Wolf.this.setTarget(null);
         super.start();
      }

      @Override
      public void tick() {
         Wolf.this.setTarget(null);
         super.tick();
      }
   }

   public static class WolfPackData extends AgeableMob.AgeableMobGroupData {
      public final Holder<WolfVariant> type;

      public WolfPackData(final Holder<WolfVariant> type) {
         super(false);
         this.type = type;
      }
   }
}
