package net.minecraft.world.entity.monster.warden;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Dynamic;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public class Warden extends Monster implements VibrationSystem {
   private static final int VIBRATION_COOLDOWN_TICKS = 40;
   private static final int TIME_TO_USE_MELEE_UNTIL_SONIC_BOOM = 200;
   private static final int MAX_HEALTH = 500;
   private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.3F;
   private static final float KNOCKBACK_RESISTANCE = 1.0F;
   private static final float ATTACK_KNOCKBACK = 1.5F;
   private static final int ATTACK_DAMAGE = 30;
   private static final int FOLLOW_RANGE = 24;
   private static final EntityDataAccessor<Integer> CLIENT_ANGER_LEVEL = SynchedEntityData.defineId(Warden.class, EntityDataSerializers.INT);
   private static final int DARKNESS_DISPLAY_LIMIT = 200;
   private static final int DARKNESS_DURATION = 260;
   private static final int DARKNESS_RADIUS = 20;
   private static final int DARKNESS_INTERVAL = 120;
   private static final int ANGERMANAGEMENT_TICK_DELAY = 20;
   private static final int DEFAULT_ANGER = 35;
   private static final int PROJECTILE_ANGER = 10;
   private static final int ON_HURT_ANGER_BOOST = 20;
   private static final int RECENT_PROJECTILE_TICK_THRESHOLD = 100;
   private static final int TOUCH_COOLDOWN_TICKS = 20;
   private static final int DIGGING_PARTICLES_AMOUNT = 30;
   private static final float DIGGING_PARTICLES_DURATION = 4.5F;
   private static final float DIGGING_PARTICLES_OFFSET = 0.7F;
   private static final int PROJECTILE_ANGER_DISTANCE = 30;
   private int tendrilAnimation;
   private int tendrilAnimationO;
   private int heartAnimation;
   private int heartAnimationO;
   public AnimationState roarAnimationState = new AnimationState();
   public AnimationState sniffAnimationState = new AnimationState();
   public AnimationState emergeAnimationState = new AnimationState();
   public AnimationState diggingAnimationState = new AnimationState();
   public AnimationState attackAnimationState = new AnimationState();
   public AnimationState sonicBoomAnimationState = new AnimationState();
   private final DynamicGameEventListener<VibrationSystem.Listener> dynamicGameEventListener;
   private final VibrationSystem.User vibrationUser;
   private VibrationSystem.Data vibrationData;
   AngerManagement angerManagement = new AngerManagement(this::canTargetEntity, Collections.emptyList());

   public Warden(EntityType<? extends Monster> p_219350_, Level p_219351_) {
      super(p_219350_, p_219351_);
      this.vibrationUser = new Warden.VibrationUser();
      this.vibrationData = new VibrationSystem.Data();
      this.dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
      this.xpReward = 5;
      this.getNavigation().setCanFloat(true);
      this.setPathfindingMalus(PathType.UNPASSABLE_RAIL, 0.0F);
      this.setPathfindingMalus(PathType.DAMAGE_OTHER, 8.0F);
      this.setPathfindingMalus(PathType.POWDER_SNOW, 8.0F);
      this.setPathfindingMalus(PathType.LAVA, 8.0F);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
      this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_342925_) {
      return new ClientboundAddEntityPacket(this, p_342925_, this.hasPose(Pose.EMERGING) ? 1 : 0);
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_219420_) {
      super.recreateFromPacket(p_219420_);
      if (p_219420_.getData() == 1) {
         this.setPose(Pose.EMERGING);
      }
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_219398_) {
      return super.checkSpawnObstruction(p_219398_) && p_219398_.noCollision(this, this.getType().getDimensions().makeBoundingBox(this.position()));
   }

   @Override
   public float getWalkTargetValue(BlockPos p_219410_, LevelReader p_219411_) {
      return 0.0F;
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel p_365950_, DamageSource p_219427_) {
      return this.isDiggingOrEmerging() && !p_219427_.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? true : super.isInvulnerableTo(p_365950_, p_219427_);
   }

   boolean isDiggingOrEmerging() {
      return this.hasPose(Pose.DIGGING) || this.hasPose(Pose.EMERGING);
   }

   @Override
   protected boolean canRide(Entity p_219462_) {
      return false;
   }

   @Override
   public float getSecondsToDisableBlocking() {
      return 5.0F;
   }

   @Override
   protected float nextStep() {
      return this.moveDist + 0.55F;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 500.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3F)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ATTACK_KNOCKBACK, 1.5)
         .add(Attributes.ATTACK_DAMAGE, 30.0)
         .add(Attributes.FOLLOW_RANGE, 24.0);
   }

   @Override
   public boolean dampensVibrations() {
      return true;
   }

   @Override
   protected float getSoundVolume() {
      return 4.0F;
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      return !this.hasPose(Pose.ROARING) && !this.isDiggingOrEmerging() ? this.getAngerLevel().getAmbientSound() : null;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_219440_) {
      return SoundEvents.WARDEN_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.WARDEN_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_219431_, BlockState p_219432_) {
      this.playSound(SoundEvents.WARDEN_STEP, 10.0F, 1.0F);
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_365355_, Entity p_219472_) {
      p_365355_.broadcastEntityEvent(this, (byte)4);
      this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 10.0F, this.getVoicePitch());
      SonicBoom.setCooldown(this, 40);
      return super.doHurtTarget(p_365355_, p_219472_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_332612_) {
      super.defineSynchedData(p_332612_);
      p_332612_.define(CLIENT_ANGER_LEVEL, 0);
   }

   public int getClientAngerLevel() {
      return this.entityData.get(CLIENT_ANGER_LEVEL);
   }

   private void syncClientAngerLevel() {
      this.entityData.set(CLIENT_ANGER_LEVEL, this.getActiveAnger());
   }

   @Override
   public void tick() {
      if (this.level() instanceof ServerLevel serverlevel) {
         VibrationSystem.Ticker.tick(serverlevel, this.vibrationData, this.vibrationUser);
         if (this.isPersistenceRequired() || this.requiresCustomPersistence()) {
            WardenAi.setDigCooldown(this);
         }
      }

      super.tick();
      if (this.level().isClientSide()) {
         if (this.tickCount % this.getHeartBeatDelay() == 0) {
            this.heartAnimation = 10;
            if (!this.isSilent()) {
               this.level()
                  .playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_HEARTBEAT, this.getSoundSource(), 5.0F, this.getVoicePitch(), false);
            }
         }

         this.tendrilAnimationO = this.tendrilAnimation;
         if (this.tendrilAnimation > 0) {
            this.tendrilAnimation--;
         }

         this.heartAnimationO = this.heartAnimation;
         if (this.heartAnimation > 0) {
            this.heartAnimation--;
         }

         switch (this.getPose()) {
            case EMERGING:
               this.clientDiggingParticles(this.emergeAnimationState);
               break;
            case DIGGING:
               this.clientDiggingParticles(this.diggingAnimationState);
         }
      }
   }

   @Override
   protected void customServerAiStep(ServerLevel p_363493_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("wardenBrain");
      this.getBrain().tick(p_363493_, this);
      profilerfiller.pop();
      super.customServerAiStep(p_363493_);
      if ((this.tickCount + this.getId()) % 120 == 0) {
         applyDarknessAround(p_363493_, this.position(), this, 20);
      }

      if (this.tickCount % 20 == 0) {
         this.angerManagement.tick(p_363493_, this::canTargetEntity);
         this.syncClientAngerLevel();
      }

      WardenAi.updateActivity(this);
   }

   @Override
   public void handleEntityEvent(byte p_219360_) {
      if (p_219360_ == 4) {
         this.roarAnimationState.stop();
         this.attackAnimationState.start(this.tickCount);
      } else if (p_219360_ == 61) {
         this.tendrilAnimation = 10;
      } else if (p_219360_ == 62) {
         this.sonicBoomAnimationState.start(this.tickCount);
      } else {
         super.handleEntityEvent(p_219360_);
      }
   }

   private int getHeartBeatDelay() {
      float f = (float)this.getClientAngerLevel() / AngerLevel.ANGRY.getMinimumAnger();
      return 40 - Mth.floor(Mth.clamp(f, 0.0F, 1.0F) * 30.0F);
   }

   public float getTendrilAnimation(float p_219468_) {
      return Mth.lerp(p_219468_, this.tendrilAnimationO, this.tendrilAnimation) / 10.0F;
   }

   public float getHeartAnimation(float p_219470_) {
      return Mth.lerp(p_219470_, this.heartAnimationO, this.heartAnimation) / 10.0F;
   }

   private void clientDiggingParticles(AnimationState p_219384_) {
      if ((float)p_219384_.getTimeInMillis(this.tickCount) < 4500.0F) {
         RandomSource randomsource = this.getRandom();
         BlockState blockstate = this.getBlockStateOn();
         if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            for (int i = 0; i < 30; i++) {
               double d0 = this.getX() + Mth.randomBetween(randomsource, -0.7F, 0.7F);
               double d1 = this.getY();
               double d2 = this.getZ() + Mth.randomBetween(randomsource, -0.7F, 0.7F);
               this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), d0, d1, d2, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_219422_) {
      if (DATA_POSE.equals(p_219422_)) {
         switch (this.getPose()) {
            case EMERGING:
               this.emergeAnimationState.start(this.tickCount);
               break;
            case DIGGING:
               this.diggingAnimationState.start(this.tickCount);
               break;
            case ROARING:
               this.roarAnimationState.start(this.tickCount);
               break;
            case SNIFFING:
               this.sniffAnimationState.start(this.tickCount);
         }
      }

      super.onSyncedDataUpdated(p_219422_);
   }

   @Override
   public boolean ignoreExplosion(Explosion p_312105_) {
      return this.isDiggingOrEmerging();
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_219406_) {
      return WardenAi.makeBrain(this, p_219406_);
   }

   @Override
   public Brain<Warden> getBrain() {
      return (Brain<Warden>)super.getBrain();
   }

   @Override
   public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> p_219413_) {
      if (this.level() instanceof ServerLevel serverlevel) {
         p_219413_.accept(this.dynamicGameEventListener, serverlevel);
      }
   }

   @Contract("null->false")
   public boolean canTargetEntity(@Nullable Entity p_219386_) {
      return p_219386_ instanceof LivingEntity livingentity
         && this.level() == p_219386_.level()
         && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p_219386_)
         && !this.isAlliedTo(p_219386_)
         && livingentity.getType() != EntityType.ARMOR_STAND
         && livingentity.getType() != EntityType.WARDEN
         && !livingentity.isInvulnerable()
         && !livingentity.isDeadOrDying()
         && this.level().getWorldBorder().isWithinBounds(livingentity.getBoundingBox());
   }

   public static void applyDarknessAround(ServerLevel p_219376_, Vec3 p_219377_, @Nullable Entity p_219378_, int p_219379_) {
      MobEffectInstance mobeffectinstance = new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false);
      MobEffectUtil.addEffectToPlayersAround(p_219376_, p_219378_, p_219377_, p_219379_, mobeffectinstance, 200);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_406329_) {
      super.addAdditionalSaveData(p_406329_);
      p_406329_.store("anger", AngerManagement.codec(this::canTargetEntity), this.angerManagement);
      p_406329_.store("listener", VibrationSystem.Data.CODEC, this.vibrationData);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_409587_) {
      super.readAdditionalSaveData(p_409587_);
      this.angerManagement = p_409587_.<AngerManagement>read("anger", AngerManagement.codec(this::canTargetEntity))
         .orElseGet(() -> new AngerManagement(this::canTargetEntity, Collections.emptyList()));
      this.syncClientAngerLevel();
      this.vibrationData = p_409587_.<VibrationSystem.Data>read("listener", VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new);
   }

   private void playListeningSound() {
      if (!this.hasPose(Pose.ROARING)) {
         this.playSound(this.getAngerLevel().getListeningSound(), 10.0F, this.getVoicePitch());
      }
   }

   public AngerLevel getAngerLevel() {
      return AngerLevel.byAnger(this.getActiveAnger());
   }

   private int getActiveAnger() {
      return this.angerManagement.getActiveAnger(this.getTarget());
   }

   public void clearAnger(Entity p_219429_) {
      this.angerManagement.clearAnger(p_219429_);
   }

   public void increaseAngerAt(@Nullable Entity p_219442_) {
      this.increaseAngerAt(p_219442_, 35, true);
   }

   @VisibleForTesting
   public void increaseAngerAt(@Nullable Entity p_219388_, int p_219389_, boolean p_219390_) {
      if (!this.isNoAi() && this.canTargetEntity(p_219388_)) {
         WardenAi.setDigCooldown(this);
         boolean flag = !(this.getTarget() instanceof Player);
         int i = this.angerManagement.increaseAnger(p_219388_, p_219389_);
         if (p_219388_ instanceof Player && flag && AngerLevel.byAnger(i).isAngry()) {
            this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
         }

         if (p_219390_) {
            this.playListeningSound();
         }
      }
   }

   public Optional<LivingEntity> getEntityAngryAt() {
      return this.getAngerLevel().isAngry() ? this.angerManagement.getActiveEntity() : Optional.empty();
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }

   @Override
   public boolean removeWhenFarAway(double p_219457_) {
      return false;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_219400_, DifficultyInstance p_219401_, EntitySpawnReason p_365792_, @Nullable SpawnGroupData p_219403_
   ) {
      this.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
      if (p_365792_ == EntitySpawnReason.TRIGGERED) {
         this.setPose(Pose.EMERGING);
         this.getBrain().setMemoryWithExpiry(MemoryModuleType.IS_EMERGING, Unit.INSTANCE, WardenAi.EMERGE_DURATION);
         this.playSound(SoundEvents.WARDEN_AGITATED, 5.0F, 1.0F);
      }

      return super.finalizeSpawn(p_219400_, p_219401_, p_365792_, p_219403_);
   }

   @Override
   public boolean hurtServer(ServerLevel p_362745_, DamageSource p_363028_, float p_361172_) {
      boolean flag = super.hurtServer(p_362745_, p_363028_, p_361172_);
      if (!this.isNoAi() && !this.isDiggingOrEmerging()) {
         Entity entity = p_363028_.getEntity();
         this.increaseAngerAt(entity, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
         if (this.brain.getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty()
            && entity instanceof LivingEntity livingentity
            && (p_363028_.isDirect() || this.closerThan(livingentity, 5.0))) {
            this.setAttackTarget(livingentity);
         }
      }

      return flag;
   }

   public void setAttackTarget(LivingEntity p_219460_) {
      this.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
      this.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, p_219460_);
      this.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      SonicBoom.setCooldown(this, 200);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_330637_) {
      EntityDimensions entitydimensions = super.getDefaultDimensions(p_330637_);
      return this.isDiggingOrEmerging() ? EntityDimensions.fixed(entitydimensions.width(), 1.0F) : entitydimensions;
   }

   @Override
   public boolean isPushable() {
      return !this.isDiggingOrEmerging() && super.isPushable();
   }

   @Override
   protected void doPush(Entity p_219353_) {
      if (!this.isNoAi() && !this.getBrain().hasMemoryValue(MemoryModuleType.TOUCH_COOLDOWN)) {
         this.getBrain().setMemoryWithExpiry(MemoryModuleType.TOUCH_COOLDOWN, Unit.INSTANCE, 20L);
         this.increaseAngerAt(p_219353_);
         WardenAi.setDisturbanceLocation(this, p_219353_.blockPosition());
      }

      super.doPush(p_219353_);
   }

   @VisibleForTesting
   public AngerManagement getAngerManagement() {
      return this.angerManagement;
   }

   @Override
   protected PathNavigation createNavigation(Level p_219396_) {
      return new GroundPathNavigation(this, p_219396_) {
         @Override
         protected PathFinder createPathFinder(int p_219479_) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            return new PathFinder(this.nodeEvaluator, p_219479_) {
               @Override
               protected float distance(Node p_219486_, Node p_219487_) {
                  return p_219486_.distanceToXZ(p_219487_);
               }
            };
         }
      };
   }

   @Override
   public VibrationSystem.Data getVibrationData() {
      return this.vibrationData;
   }

   @Override
   public VibrationSystem.User getVibrationUser() {
      return this.vibrationUser;
   }

   class VibrationUser implements VibrationSystem.User {
      private static final int GAME_EVENT_LISTENER_RANGE = 16;
      private final PositionSource positionSource = new EntityPositionSource(Warden.this, Warden.this.getEyeHeight());

      @Override
      public int getListenerRadius() {
         return 16;
      }

      @Override
      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      @Override
      public TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.WARDEN_CAN_LISTEN;
      }

      @Override
      public boolean canTriggerAvoidVibration() {
         return true;
      }

      @Override
      public boolean canReceiveVibration(ServerLevel p_282574_, BlockPos p_282323_, Holder<GameEvent> p_330632_, GameEvent.Context p_282515_) {
         return !Warden.this.isNoAi()
               && !Warden.this.isDeadOrDying()
               && !Warden.this.getBrain().hasMemoryValue(MemoryModuleType.VIBRATION_COOLDOWN)
               && !Warden.this.isDiggingOrEmerging()
               && p_282574_.getWorldBorder().isWithinBounds(p_282323_)
            ? !(p_282515_.sourceEntity() instanceof LivingEntity livingentity && !Warden.this.canTargetEntity(livingentity))
            : false;
      }

      @Override
      public void onReceiveVibration(
         ServerLevel p_281325_, BlockPos p_282386_, Holder<GameEvent> p_329564_, @Nullable Entity p_281438_, @Nullable Entity p_282582_, float p_283699_
      ) {
         if (!Warden.this.isDeadOrDying()) {
            Warden.this.brain.setMemoryWithExpiry(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, 40L);
            p_281325_.broadcastEntityEvent(Warden.this, (byte)61);
            Warden.this.playSound(SoundEvents.WARDEN_TENDRIL_CLICKS, 5.0F, Warden.this.getVoicePitch());
            BlockPos blockpos = p_282386_;
            if (p_282582_ != null) {
               if (Warden.this.closerThan(p_282582_, 30.0)) {
                  if (Warden.this.getBrain().hasMemoryValue(MemoryModuleType.RECENT_PROJECTILE)) {
                     if (Warden.this.canTargetEntity(p_282582_)) {
                        blockpos = p_282582_.blockPosition();
                     }

                     Warden.this.increaseAngerAt(p_282582_);
                  } else {
                     Warden.this.increaseAngerAt(p_282582_, 10, true);
                  }
               }

               Warden.this.getBrain().setMemoryWithExpiry(MemoryModuleType.RECENT_PROJECTILE, Unit.INSTANCE, 100L);
            } else {
               Warden.this.increaseAngerAt(p_281438_);
            }

            if (!Warden.this.getAngerLevel().isAngry()) {
               Optional<LivingEntity> optional = Warden.this.angerManagement.getActiveEntity();
               if (p_282582_ != null || optional.isEmpty() || optional.get() == p_281438_) {
                  WardenAi.setDisturbanceLocation(Warden.this, blockpos);
               }
            }
         }
      }
   }
}
