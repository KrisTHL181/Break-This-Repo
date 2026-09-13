package net.minecraft.world.entity.animal.axolotl;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.BinaryAnimator;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.EasingType;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Axolotl extends Animal implements Bucketable {
   public static final int TOTAL_PLAYDEAD_TIME = 200;
   private static final int POSE_ANIMATION_TICKS = 10;
   protected static final ImmutableList<? extends SensorType<? extends Sensor<? super Axolotl>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_ADULT, SensorType.HURT_BY, SensorType.AXOLOTL_ATTACKABLES, SensorType.FOOD_TEMPTATIONS
   );
   protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.BREED_TARGET,
      MemoryModuleType.NEAREST_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
      MemoryModuleType.LOOK_TARGET,
      MemoryModuleType.WALK_TARGET,
      MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
      MemoryModuleType.PATH,
      MemoryModuleType.ATTACK_TARGET,
      MemoryModuleType.ATTACK_COOLING_DOWN,
      MemoryModuleType.NEAREST_VISIBLE_ADULT,
      new MemoryModuleType[]{
         MemoryModuleType.HURT_BY_ENTITY,
         MemoryModuleType.PLAY_DEAD_TICKS,
         MemoryModuleType.NEAREST_ATTACKABLE,
         MemoryModuleType.TEMPTING_PLAYER,
         MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
         MemoryModuleType.IS_TEMPTED,
         MemoryModuleType.HAS_HUNTING_COOLDOWN,
         MemoryModuleType.IS_PANICKING
      }
   );
   private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(Axolotl.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_PLAYING_DEAD = SynchedEntityData.defineId(Axolotl.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(Axolotl.class, EntityDataSerializers.BOOLEAN);
   public static final double PLAYER_REGEN_DETECTION_RANGE = 20.0;
   public static final int RARE_VARIANT_CHANCE = 1200;
   private static final int AXOLOTL_TOTAL_AIR_SUPPLY = 6000;
   public static final String VARIANT_TAG = "Variant";
   private static final int REHYDRATE_AIR_SUPPLY = 1800;
   private static final int REGEN_BUFF_MAX_DURATION = 2400;
   private static final boolean DEFAULT_FROM_BUCKET = false;
   public final BinaryAnimator playingDeadAnimator = new BinaryAnimator(10, EasingType.IN_OUT_SINE);
   public final BinaryAnimator inWaterAnimator = new BinaryAnimator(10, EasingType.IN_OUT_SINE);
   public final BinaryAnimator onGroundAnimator = new BinaryAnimator(10, EasingType.IN_OUT_SINE);
   public final BinaryAnimator movingAnimator = new BinaryAnimator(10, EasingType.IN_OUT_SINE);
   private static final int REGEN_BUFF_BASE_DURATION = 100;

   public Axolotl(EntityType<? extends Axolotl> p_149105_, Level p_149106_) {
      super(p_149105_, p_149106_);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
      this.moveControl = new Axolotl.AxolotlMoveControl(this);
      this.lookControl = new Axolotl.AxolotlLookControl(this, 20);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_149140_, LevelReader p_149141_) {
      return 0.0F;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_333789_) {
      super.defineSynchedData(p_333789_);
      p_333789_.define(DATA_VARIANT, 0);
      p_333789_.define(DATA_PLAYING_DEAD, false);
      p_333789_.define(FROM_BUCKET, false);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_406247_) {
      super.addAdditionalSaveData(p_406247_);
      p_406247_.store("Variant", Axolotl.Variant.LEGACY_CODEC, this.getVariant());
      p_406247_.putBoolean("FromBucket", this.fromBucket());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_410258_) {
      super.readAdditionalSaveData(p_410258_);
      this.setVariant(p_410258_.<Axolotl.Variant>read("Variant", Axolotl.Variant.LEGACY_CODEC).orElse(Axolotl.Variant.DEFAULT));
      this.setFromBucket(p_410258_.getBooleanOr("FromBucket", false));
   }

   @Override
   public void playAmbientSound() {
      if (!this.isPlayingDead()) {
         super.playAmbientSound();
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_149132_, DifficultyInstance p_149133_, EntitySpawnReason p_363665_, @Nullable SpawnGroupData p_149135_
   ) {
      boolean flag = false;
      if (p_363665_ == EntitySpawnReason.BUCKET) {
         return p_149135_;
      }

      RandomSource randomsource = p_149132_.getRandom();
      if (p_149135_ instanceof Axolotl.AxolotlGroupData) {
         if (((Axolotl.AxolotlGroupData)p_149135_).getGroupSize() >= 2) {
            flag = true;
         }
      } else {
         p_149135_ = new Axolotl.AxolotlGroupData(Axolotl.Variant.getCommonSpawnVariant(randomsource), Axolotl.Variant.getCommonSpawnVariant(randomsource));
      }

      this.setVariant(((Axolotl.AxolotlGroupData)p_149135_).getVariant(randomsource));
      if (flag) {
         this.setAge(-24000);
      }

      return super.finalizeSpawn(p_149132_, p_149133_, p_363665_, p_149135_);
   }

   @Override
   public void baseTick() {
      int i = this.getAirSupply();
      super.baseTick();
      if (!this.isNoAi() && this.level() instanceof ServerLevel serverlevel) {
         this.handleAirSupply(serverlevel, i);
      }

      if (this.level().isClientSide()) {
         this.tickAnimations();
      }
   }

   private void tickAnimations() {
      Axolotl.AnimationState axolotl$animationstate;
      if (this.isPlayingDead()) {
         axolotl$animationstate = Axolotl.AnimationState.PLAYING_DEAD;
      } else if (this.isInWater()) {
         axolotl$animationstate = Axolotl.AnimationState.IN_WATER;
      } else if (this.onGround()) {
         axolotl$animationstate = Axolotl.AnimationState.ON_GROUND;
      } else {
         axolotl$animationstate = Axolotl.AnimationState.IN_AIR;
      }

      this.playingDeadAnimator.tick(axolotl$animationstate == Axolotl.AnimationState.PLAYING_DEAD);
      this.inWaterAnimator.tick(axolotl$animationstate == Axolotl.AnimationState.IN_WATER);
      this.onGroundAnimator.tick(axolotl$animationstate == Axolotl.AnimationState.ON_GROUND);
      boolean flag = this.walkAnimation.isMoving() || this.getXRot() != this.xRotO || this.getYRot() != this.yRotO;
      this.movingAnimator.tick(flag);
   }

   protected void handleAirSupply(ServerLevel p_392414_, int p_149194_) {
      if (this.isAlive() && !this.isInWaterOrRain()) {
         this.setAirSupply(p_149194_ - 1);
         if (this.shouldTakeDrowningDamage()) {
            this.setAirSupply(0);
            this.hurtServer(p_392414_, this.damageSources().dryOut(), 2.0F);
         }
      } else {
         this.setAirSupply(this.getMaxAirSupply());
      }
   }

   public void rehydrate() {
      int i = this.getAirSupply() + 1800;
      this.setAirSupply(Math.min(i, this.getMaxAirSupply()));
   }

   @Override
   public int getMaxAirSupply() {
      return 6000;
   }

   public Axolotl.Variant getVariant() {
      return Axolotl.Variant.byId(this.entityData.get(DATA_VARIANT));
   }

   private void setVariant(Axolotl.Variant p_149118_) {
      this.entityData.set(DATA_VARIANT, p_149118_.getId());
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_395228_) {
      return p_395228_ == DataComponents.AXOLOTL_VARIANT ? castComponentValue((DataComponentType<T>)p_395228_, this.getVariant()) : super.get(p_395228_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_394337_) {
      this.applyImplicitComponentIfPresent(p_394337_, DataComponents.AXOLOTL_VARIANT);
      super.applyImplicitComponents(p_394337_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_393781_, T p_392488_) {
      if (p_393781_ == DataComponents.AXOLOTL_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.AXOLOTL_VARIANT, p_392488_));
         return true;
      } else {
         return super.applyImplicitComponent(p_393781_, p_392488_);
      }
   }

   private static boolean useRareVariant(RandomSource p_218436_) {
      return p_218436_.nextInt(1200) == 0;
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_149130_) {
      return p_149130_.isUnobstructed(this);
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   public void setPlayingDead(boolean p_149199_) {
      this.entityData.set(DATA_PLAYING_DEAD, p_149199_);
   }

   public boolean isPlayingDead() {
      return this.entityData.get(DATA_PLAYING_DEAD);
   }

   @Override
   public boolean fromBucket() {
      return this.entityData.get(FROM_BUCKET);
   }

   @Override
   public void setFromBucket(boolean p_149196_) {
      this.entityData.set(FROM_BUCKET, p_149196_);
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_149112_, AgeableMob p_149113_) {
      Axolotl axolotl = EntityType.AXOLOTL.create(p_149112_, EntitySpawnReason.BREEDING);
      if (axolotl != null) {
         Axolotl.Variant axolotl$variant;
         if (useRareVariant(this.random)) {
            axolotl$variant = Axolotl.Variant.getRareSpawnVariant(this.random);
         } else {
            axolotl$variant = this.random.nextBoolean() ? this.getVariant() : ((Axolotl)p_149113_).getVariant();
         }

         axolotl.setVariant(axolotl$variant);
         axolotl.setPersistenceRequired();
      }

      return axolotl;
   }

   @Override
   public boolean isFood(ItemStack p_149189_) {
      return p_149189_.is(ItemTags.AXOLOTL_FOOD);
   }

   @Override
   public boolean canBeLeashed() {
      return true;
   }

   @Override
   protected void customServerAiStep(ServerLevel p_361350_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("axolotlBrain");
      this.getBrain().tick(p_361350_, this);
      profilerfiller.pop();
      profilerfiller.push("axolotlActivityUpdate");
      AxolotlAi.updateActivity(this);
      profilerfiller.pop();
      if (!this.isNoAi()) {
         Optional<Integer> optional = this.getBrain().getMemory(MemoryModuleType.PLAY_DEAD_TICKS);
         this.setPlayingDead(optional.isPresent() && optional.get() > 0);
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes()
         .add(Attributes.MAX_HEALTH, 14.0)
         .add(Attributes.MOVEMENT_SPEED, 1.0)
         .add(Attributes.ATTACK_DAMAGE, 2.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected PathNavigation createNavigation(Level p_149128_) {
      return new AmphibiousPathNavigation(this, p_149128_);
   }

   @Override
   public void playAttackSound() {
      this.playSound(SoundEvents.AXOLOTL_ATTACK, 1.0F, 1.0F);
   }

   @Override
   public boolean hurtServer(ServerLevel p_367856_, DamageSource p_361956_, float p_365535_) {
      float f = this.getHealth();
      if (!this.isNoAi()
         && this.level().random.nextInt(3) == 0
         && (this.level().random.nextInt(3) < p_365535_ || f / this.getMaxHealth() < 0.5F)
         && p_365535_ < f
         && this.isInWater()
         && (p_361956_.getEntity() != null || p_361956_.getDirectEntity() != null)
         && !this.isPlayingDead()) {
         this.brain.setMemory(MemoryModuleType.PLAY_DEAD_TICKS, 200);
      }

      return super.hurtServer(p_367856_, p_361956_, p_365535_);
   }

   @Override
   public int getMaxHeadXRot() {
      return 1;
   }

   @Override
   public int getMaxHeadYRot() {
      return 1;
   }

   @Override
   public InteractionResult mobInteract(Player p_149155_, InteractionHand p_149156_) {
      return Bucketable.bucketMobPickup(p_149155_, p_149156_, this).orElse(super.mobInteract(p_149155_, p_149156_));
   }

   @Override
   public void saveToBucketTag(ItemStack p_149187_) {
      Bucketable.saveDefaultDataToBucketTag(this, p_149187_);
      p_149187_.copyFrom(DataComponents.AXOLOTL_VARIANT, this);
      CustomData.update(DataComponents.BUCKET_ENTITY_DATA, p_149187_, p_449658_ -> {
         p_449658_.putInt("Age", this.getAge());
         Brain<?> brain = this.getBrain();
         if (brain.hasMemoryValue(MemoryModuleType.HAS_HUNTING_COOLDOWN)) {
            p_449658_.putLong("HuntingCooldown", brain.getTimeUntilExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN));
         }
      });
   }

   @Override
   public void loadFromBucketTag(CompoundTag p_149163_) {
      Bucketable.loadDefaultDataFromBucketTag(this, p_149163_);
      this.setAge(p_149163_.getIntOr("Age", 0));
      p_149163_.getLong("HuntingCooldown")
         .ifPresentOrElse(
            p_390661_ -> this.getBrain().setMemoryWithExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN, true, p_149163_.getLongOr("HuntingCooldown", 0L)),
            () -> this.getBrain().setMemory(MemoryModuleType.HAS_HUNTING_COOLDOWN, Optional.empty())
         );
   }

   @Override
   public ItemStack getBucketItemStack() {
      return new ItemStack(Items.AXOLOTL_BUCKET);
   }

   @Override
   public SoundEvent getPickupSound() {
      return SoundEvents.BUCKET_FILL_AXOLOTL;
   }

   @Override
   public boolean canBeSeenAsEnemy() {
      return !this.isPlayingDead() && super.canBeSeenAsEnemy();
   }

   public static void onStopAttacking(ServerLevel p_365297_, Axolotl p_218444_, LivingEntity p_218445_) {
      if (p_218445_.isDeadOrDying()) {
         DamageSource damagesource = p_218445_.getLastDamageSource();
         if (damagesource != null) {
            Entity entity = damagesource.getEntity();
            if (entity != null && entity.getType() == EntityType.PLAYER) {
               Player player = (Player)entity;
               List<Player> list = p_365297_.getEntitiesOfClass(Player.class, p_218444_.getBoundingBox().inflate(20.0));
               if (list.contains(player)) {
                  p_218444_.applySupportingEffects(player);
               }
            }
         }
      }
   }

   public void applySupportingEffects(Player p_149174_) {
      MobEffectInstance mobeffectinstance = p_149174_.getEffect(MobEffects.REGENERATION);
      if (mobeffectinstance == null || mobeffectinstance.endsWithin(2399)) {
         int i = mobeffectinstance != null ? mobeffectinstance.getDuration() : 0;
         int j = Math.min(2400, 100 + i);
         p_149174_.addEffect(new MobEffectInstance(MobEffects.REGENERATION, j, 0), this);
      }

      p_149174_.removeEffect(MobEffects.MINING_FATIGUE);
   }

   @Override
   public boolean requiresCustomPersistence() {
      return super.requiresCustomPersistence() || this.fromBucket();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_149161_) {
      return SoundEvents.AXOLOTL_HURT;
   }

   @Override
   protected @Nullable SoundEvent getDeathSound() {
      return SoundEvents.AXOLOTL_DEATH;
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      return this.isInWater() ? SoundEvents.AXOLOTL_IDLE_WATER : SoundEvents.AXOLOTL_IDLE_AIR;
   }

   @Override
   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.AXOLOTL_SPLASH;
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.AXOLOTL_SWIM;
   }

   @Override
   protected Brain.Provider<Axolotl> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_149138_) {
      return AxolotlAi.makeBrain(this.brainProvider().makeBrain(p_149138_));
   }

   @Override
   public Brain<Axolotl> getBrain() {
      return (Brain<Axolotl>)super.getBrain();
   }

   @Override
   protected void travelInWater(Vec3 p_457818_, double p_451499_, boolean p_456739_, double p_456083_) {
      this.moveRelative(this.getSpeed(), p_457818_);
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
   }

   @Override
   protected void usePlayerItem(Player p_149124_, InteractionHand p_149125_, ItemStack p_149126_) {
      if (p_149126_.is(Items.TROPICAL_FISH_BUCKET)) {
         p_149124_.setItemInHand(p_149125_, ItemUtils.createFilledResult(p_149126_, p_149124_, new ItemStack(Items.WATER_BUCKET)));
      } else {
         super.usePlayerItem(p_149124_, p_149125_, p_149126_);
      }
   }

   @Override
   public boolean removeWhenFarAway(double p_149183_) {
      return !this.fromBucket() && !this.hasCustomName();
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }

   public static boolean checkAxolotlSpawnRules(
      EntityType<? extends LivingEntity> p_218438_, ServerLevelAccessor p_218439_, EntitySpawnReason p_367518_, BlockPos p_218441_, RandomSource p_218442_
   ) {
      return p_218439_.getBlockState(p_218441_.below()).is(BlockTags.AXOLOTLS_SPAWNABLE_ON);
   }

   public enum AnimationState {
      PLAYING_DEAD,
      IN_WATER,
      ON_GROUND,
      IN_AIR;
   }

   public static class AxolotlGroupData extends AgeableMob.AgeableMobGroupData {
      public final Axolotl.Variant[] types;

      public AxolotlGroupData(Axolotl.Variant... p_149204_) {
         super(false);
         this.types = p_149204_;
      }

      public Axolotl.Variant getVariant(RandomSource p_218447_) {
         return this.types[p_218447_.nextInt(this.types.length)];
      }
   }

   class AxolotlLookControl extends SmoothSwimmingLookControl {
      public AxolotlLookControl(final Axolotl p_149210_, final int p_149211_) {
         super(p_149210_, p_149211_);
      }

      @Override
      public void tick() {
         if (!Axolotl.this.isPlayingDead()) {
            super.tick();
         }
      }
   }

   static class AxolotlMoveControl extends SmoothSwimmingMoveControl {
      private final Axolotl axolotl;

      public AxolotlMoveControl(Axolotl p_149215_) {
         super(p_149215_, 85, 10, 0.1F, 0.5F, false);
         this.axolotl = p_149215_;
      }

      @Override
      public void tick() {
         if (!this.axolotl.isPlayingDead()) {
            super.tick();
         }
      }
   }

   public enum Variant implements StringRepresentable {
      LUCY(0, "lucy", true),
      WILD(1, "wild", true),
      GOLD(2, "gold", true),
      CYAN(3, "cyan", true),
      BLUE(4, "blue", false);

      public static final Axolotl.Variant DEFAULT = LUCY;
      private static final IntFunction<Axolotl.Variant> BY_ID = ByIdMap.continuous(Axolotl.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      public static final StreamCodec<ByteBuf, Axolotl.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Axolotl.Variant::getId);
      public static final Codec<Axolotl.Variant> CODEC = StringRepresentable.fromEnum(Axolotl.Variant::values);
      @Deprecated
      public static final Codec<Axolotl.Variant> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, Axolotl.Variant::getId);
      private final int id;
      private final String name;
      private final boolean common;

      Variant(final int p_149239_, final String p_149240_, final boolean p_149241_) {
         this.id = p_149239_;
         this.name = p_149240_;
         this.common = p_149241_;
      }

      public int getId() {
         return this.id;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public static Axolotl.Variant byId(int p_262930_) {
         return BY_ID.apply(p_262930_);
      }

      public static Axolotl.Variant getCommonSpawnVariant(RandomSource p_218449_) {
         return getSpawnVariant(p_218449_, true);
      }

      public static Axolotl.Variant getRareSpawnVariant(RandomSource p_218454_) {
         return getSpawnVariant(p_218454_, false);
      }

      private static Axolotl.Variant getSpawnVariant(RandomSource p_218451_, boolean p_218452_) {
         Axolotl.Variant[] aaxolotl$variant = Arrays.stream(values()).filter(p_149252_ -> p_149252_.common == p_218452_).toArray(Axolotl.Variant[]::new);
         return Util.getRandom(aaxolotl$variant, p_218451_);
      }
   }
}
