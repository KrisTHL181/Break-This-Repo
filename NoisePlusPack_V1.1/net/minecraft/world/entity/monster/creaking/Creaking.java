package net.minecraft.world.entity.monster.creaking;

import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Creaking extends Monster {
   private static final EntityDataAccessor<Boolean> CAN_MOVE = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> IS_ACTIVE = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> IS_TEARING_DOWN = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Optional<BlockPos>> HOME_POS = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
   private static final int ATTACK_ANIMATION_DURATION = 15;
   private static final int MAX_HEALTH = 1;
   private static final float ATTACK_DAMAGE = 3.0F;
   private static final float FOLLOW_RANGE = 32.0F;
   private static final float ACTIVATION_RANGE_SQ = 144.0F;
   public static final int ATTACK_INTERVAL = 40;
   private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.4F;
   public static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.3F;
   public static final int CREAKING_ORANGE = 16545810;
   public static final int CREAKING_GRAY = 6250335;
   public static final int INVULNERABILITY_ANIMATION_DURATION = 8;
   public static final int TWITCH_DEATH_DURATION = 45;
   private static final int MAX_PLAYER_STUCK_COUNTER = 4;
   private int attackAnimationRemainingTicks;
   public final AnimationState attackAnimationState = new AnimationState();
   public final AnimationState invulnerabilityAnimationState = new AnimationState();
   public final AnimationState deathAnimationState = new AnimationState();
   private int invulnerabilityAnimationRemainingTicks;
   private boolean eyesGlowing;
   private int nextFlickerTime;
   private int playerStuckCounter;

   public Creaking(EntityType<? extends Creaking> p_368209_, Level p_365865_) {
      super(p_368209_, p_365865_);
      this.lookControl = new Creaking.CreakingLookControl(this);
      this.moveControl = new Creaking.CreakingMoveControl(this);
      this.jumpControl = new Creaking.CreakingJumpControl(this);
      GroundPathNavigation groundpathnavigation = (GroundPathNavigation)this.getNavigation();
      groundpathnavigation.setCanFloat(true);
      this.xpReward = 0;
   }

   public void setTransient(BlockPos p_376073_) {
      this.setHomePos(p_376073_);
      this.setPathfindingMalus(PathType.DAMAGE_OTHER, 8.0F);
      this.setPathfindingMalus(PathType.POWDER_SNOW, 8.0F);
      this.setPathfindingMalus(PathType.LAVA, 8.0F);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
      this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
   }

   public boolean isHeartBound() {
      return this.getHomePos() != null;
   }

   @Override
   protected BodyRotationControl createBodyControl() {
      return new Creaking.CreakingBodyRotationControl(this);
   }

   @Override
   protected Brain.Provider<Creaking> brainProvider() {
      return CreakingAi.brainProvider();
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_363322_) {
      return CreakingAi.makeBrain(this, this.brainProvider().makeBrain(p_363322_));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_364477_) {
      super.defineSynchedData(p_364477_);
      p_364477_.define(CAN_MOVE, true);
      p_364477_.define(IS_ACTIVE, false);
      p_364477_.define(IS_TEARING_DOWN, false);
      p_364477_.define(HOME_POS, Optional.empty());
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 1.0)
         .add(Attributes.MOVEMENT_SPEED, 0.4F)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.STEP_HEIGHT, 1.0625);
   }

   public boolean canMove() {
      return this.entityData.get(CAN_MOVE);
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_363767_, Entity p_362044_) {
      if (!(p_362044_ instanceof LivingEntity)) {
         return false;
      }

      this.attackAnimationRemainingTicks = 15;
      this.level().broadcastEntityEvent(this, (byte)4);
      return super.doHurtTarget(p_363767_, p_362044_);
   }

   @Override
   public boolean hurtServer(ServerLevel p_375862_, DamageSource p_377615_, float p_376232_) {
      BlockPos blockpos = this.getHomePos();
      if (blockpos == null || p_377615_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return super.hurtServer(p_375862_, p_377615_, p_376232_);
      }

      if (!this.isInvulnerableTo(p_375862_, p_377615_) && this.invulnerabilityAnimationRemainingTicks <= 0 && !this.isDeadOrDying()) {
         Player player = this.blameSourceForDamage(p_377615_);
         Entity entity = p_377615_.getDirectEntity();
         if (!(entity instanceof LivingEntity) && !(entity instanceof Projectile) && player == null) {
            return false;
         }

         this.invulnerabilityAnimationRemainingTicks = 8;
         this.level().broadcastEntityEvent(this, (byte)66);
         this.gameEvent(GameEvent.ENTITY_ACTION);
         if (this.level().getBlockEntity(blockpos) instanceof CreakingHeartBlockEntity creakingheartblockentity && creakingheartblockentity.isProtector(this)) {
            if (player != null) {
               creakingheartblockentity.creakingHurt();
            }

            this.playHurtSound(p_377615_);
         }

         return true;
      } else {
         return false;
      }
   }

   public Player blameSourceForDamage(DamageSource p_375908_) {
      this.resolveMobResponsibleForDamage(p_375908_);
      return this.resolvePlayerResponsibleForDamage(p_375908_);
   }

   @Override
   public boolean isPushable() {
      return super.isPushable() && this.canMove();
   }

   @Override
   public void push(double p_376827_, double p_376076_, double p_377600_) {
      if (this.canMove()) {
         super.push(p_376827_, p_376076_, p_377600_);
      }
   }

   @Override
   public Brain<Creaking> getBrain() {
      return (Brain<Creaking>)super.getBrain();
   }

   @Override
   protected void customServerAiStep(ServerLevel p_365221_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("creakingBrain");
      this.getBrain().tick((ServerLevel)this.level(), this);
      profilerfiller.pop();
      CreakingAi.updateActivity(this);
   }

   @Override
   public void aiStep() {
      if (this.invulnerabilityAnimationRemainingTicks > 0) {
         this.invulnerabilityAnimationRemainingTicks--;
      }

      if (this.attackAnimationRemainingTicks > 0) {
         this.attackAnimationRemainingTicks--;
      }

      if (!this.level().isClientSide()) {
         boolean flag = this.entityData.get(CAN_MOVE);
         boolean flag1 = this.checkCanMove();
         if (flag1 != flag) {
            this.gameEvent(GameEvent.ENTITY_ACTION);
            if (flag1) {
               this.makeSound(SoundEvents.CREAKING_UNFREEZE);
            } else {
               this.stopInPlace();
               this.makeSound(SoundEvents.CREAKING_FREEZE);
            }
         }

         this.entityData.set(CAN_MOVE, flag1);
      }

      super.aiStep();
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide()) {
         BlockPos blockpos = this.getHomePos();
         if (blockpos != null) {
            boolean flag = this.level().getBlockEntity(blockpos) instanceof CreakingHeartBlockEntity creakingheartblockentity
               && creakingheartblockentity.isProtector(this);
            if (!flag) {
               this.setHealth(0.0F);
            }
         }
      }

      super.tick();
      if (this.level().isClientSide()) {
         this.setupAnimationStates();
         this.checkEyeBlink();
      }
   }

   @Override
   protected void tickDeath() {
      if (this.isHeartBound() && this.isTearingDown()) {
         this.deathTime++;
         if (!this.level().isClientSide() && this.deathTime > 45 && !this.isRemoved()) {
            this.tearDown();
         }
      } else {
         super.tickDeath();
      }
   }

   @Override
   protected void updateWalkAnimation(float p_377533_) {
      float f = Math.min(p_377533_ * 25.0F, 3.0F);
      this.walkAnimation.update(f, 0.4F, 1.0F);
   }

   private void setupAnimationStates() {
      this.attackAnimationState.animateWhen(this.attackAnimationRemainingTicks > 0, this.tickCount);
      this.invulnerabilityAnimationState.animateWhen(this.invulnerabilityAnimationRemainingTicks > 0, this.tickCount);
      this.deathAnimationState.animateWhen(this.isTearingDown(), this.tickCount);
   }

   public void tearDown() {
      if (this.level() instanceof ServerLevel serverlevel) {
         AABB aabb = this.getBoundingBox();
         Vec3 vec3 = aabb.getCenter();
         double d0 = aabb.getXsize() * 0.3;
         double d1 = aabb.getYsize() * 0.3;
         double d2 = aabb.getZsize() * 0.3;
         serverlevel.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, Blocks.PALE_OAK_WOOD.defaultBlockState()), vec3.x, vec3.y, vec3.z, 100, d0, d1, d2, 0.0
         );
         serverlevel.sendParticles(
            new BlockParticleOption(
               ParticleTypes.BLOCK_CRUMBLE, Blocks.CREAKING_HEART.defaultBlockState().setValue(CreakingHeartBlock.STATE, CreakingHeartState.AWAKE)
            ),
            vec3.x,
            vec3.y,
            vec3.z,
            10,
            d0,
            d1,
            d2,
            0.0
         );
      }

      this.makeSound(this.getDeathSound());
      this.remove(Entity.RemovalReason.DISCARDED);
   }

   public void creakingDeathEffects(DamageSource p_377960_) {
      this.blameSourceForDamage(p_377960_);
      this.die(p_377960_);
      this.makeSound(SoundEvents.CREAKING_TWITCH);
   }

   @Override
   public void handleEntityEvent(byte p_369330_) {
      if (p_369330_ == 66) {
         this.invulnerabilityAnimationRemainingTicks = 8;
         this.playHurtSound(this.damageSources().generic());
      } else if (p_369330_ == 4) {
         this.attackAnimationRemainingTicks = 15;
         this.playAttackSound();
      } else {
         super.handleEntityEvent(p_369330_);
      }
   }

   @Override
   public boolean fireImmune() {
      return this.isHeartBound() || super.fireImmune();
   }

   @Override
   public boolean canUsePortal(boolean p_375539_) {
      return !this.isHeartBound() && super.canUsePortal(p_375539_);
   }

   @Override
   protected PathNavigation createNavigation(Level p_378684_) {
      return new Creaking.CreakingPathNavigation(this, p_378684_);
   }

   public boolean playerIsStuckInYou() {
      List<Player> list = this.brain.getMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
      if (list.isEmpty()) {
         this.playerStuckCounter = 0;
         return false;
      }

      AABB aabb = this.getBoundingBox();

      for (Player player : list) {
         if (aabb.contains(player.getEyePosition())) {
            this.playerStuckCounter++;
            return this.playerStuckCounter > 4;
         }
      }

      this.playerStuckCounter = 0;
      return false;
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_408192_) {
      super.readAdditionalSaveData(p_408192_);
      p_408192_.<BlockPos>read("home_pos", BlockPos.CODEC).ifPresent(this::setTransient);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_408692_) {
      super.addAdditionalSaveData(p_408692_);
      p_408692_.storeNullable("home_pos", BlockPos.CODEC, this.getHomePos());
   }

   public void setHomePos(BlockPos p_376376_) {
      this.entityData.set(HOME_POS, Optional.of(p_376376_));
   }

   public @Nullable BlockPos getHomePos() {
      return this.entityData.get(HOME_POS).orElse(null);
   }

   public void setTearingDown() {
      this.entityData.set(IS_TEARING_DOWN, true);
   }

   public boolean isTearingDown() {
      return this.entityData.get(IS_TEARING_DOWN);
   }

   public boolean hasGlowingEyes() {
      return this.eyesGlowing;
   }

   public void checkEyeBlink() {
      if (this.deathTime > this.nextFlickerTime) {
         this.nextFlickerTime = this.deathTime
            + this.getRandom().nextIntBetweenInclusive(this.eyesGlowing ? 2 : this.deathTime / 4, this.eyesGlowing ? 8 : this.deathTime / 2);
         this.eyesGlowing = !this.eyesGlowing;
      }
   }

   @Override
   public void playAttackSound() {
      this.makeSound(SoundEvents.CREAKING_ATTACK);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isActive() ? null : SoundEvents.CREAKING_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_368268_) {
      return this.isHeartBound() ? SoundEvents.CREAKING_SWAY : super.getHurtSound(p_368268_);
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CREAKING_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_367090_, BlockState p_366342_) {
      this.playSound(SoundEvents.CREAKING_STEP, 0.15F, 1.0F);
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }

   @Override
   public void knockback(double p_364942_, double p_368513_, double p_364343_) {
      if (this.canMove()) {
         super.knockback(p_364942_, p_368513_, p_364343_);
      }
   }

   public boolean checkCanMove() {
      List<Player> list = this.brain.getMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
      boolean flag = this.isActive();
      if (list.isEmpty()) {
         if (flag) {
            this.deactivate();
         }

         return true;
      } else {
         boolean flag1 = false;

         for (Player player : list) {
            if (this.canAttack(player) && !this.isAlliedTo(player)) {
               flag1 = true;
               if ((!flag || LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM.test(player))
                  && this.isLookingAtMe(player, 0.5, false, true, this.getEyeY(), this.getY() + 0.5 * this.getScale(), (this.getEyeY() + this.getY()) / 2.0)) {
                  if (flag) {
                     return false;
                  }

                  if (player.distanceToSqr(this) < 144.0) {
                     this.activate(player);
                     return false;
                  }
               }
            }
         }

         if (!flag1 && flag) {
            this.deactivate();
         }

         return true;
      }
   }

   public void activate(Player p_376101_) {
      this.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, p_376101_);
      this.gameEvent(GameEvent.ENTITY_ACTION);
      this.makeSound(SoundEvents.CREAKING_ACTIVATE);
      this.setIsActive(true);
   }

   public void deactivate() {
      this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
      this.gameEvent(GameEvent.ENTITY_ACTION);
      this.makeSound(SoundEvents.CREAKING_DEACTIVATE);
      this.setIsActive(false);
   }

   public void setIsActive(boolean p_366535_) {
      this.entityData.set(IS_ACTIVE, p_366535_);
   }

   public boolean isActive() {
      return this.entityData.get(IS_ACTIVE);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_368461_, LevelReader p_365406_) {
      return 0.0F;
   }

   class CreakingBodyRotationControl extends BodyRotationControl {
      public CreakingBodyRotationControl(final Creaking p_364588_) {
         super(p_364588_);
      }

      @Override
      public void clientTick() {
         if (Creaking.this.canMove()) {
            super.clientTick();
         }
      }
   }

   class CreakingJumpControl extends JumpControl {
      public CreakingJumpControl(final Creaking p_368023_) {
         super(p_368023_);
      }

      @Override
      public void tick() {
         if (Creaking.this.canMove()) {
            super.tick();
         } else {
            Creaking.this.setJumping(false);
         }
      }
   }

   class CreakingLookControl extends LookControl {
      public CreakingLookControl(final Creaking p_367583_) {
         super(p_367583_);
      }

      @Override
      public void tick() {
         if (Creaking.this.canMove()) {
            super.tick();
         }
      }
   }

   class CreakingMoveControl extends MoveControl {
      public CreakingMoveControl(final Creaking p_365109_) {
         super(p_365109_);
      }

      @Override
      public void tick() {
         if (Creaking.this.canMove()) {
            super.tick();
         }
      }
   }

   class CreakingPathNavigation extends GroundPathNavigation {
      CreakingPathNavigation(final Creaking p_377680_, final Level p_375587_) {
         super(p_377680_, p_375587_);
      }

      @Override
      public void tick() {
         if (Creaking.this.canMove()) {
            super.tick();
         }
      }

      @Override
      protected PathFinder createPathFinder(int p_378313_) {
         this.nodeEvaluator = Creaking.this.new HomeNodeEvaluator();
         this.nodeEvaluator.setCanPassDoors(true);
         return new PathFinder(this.nodeEvaluator, p_378313_);
      }
   }

   class HomeNodeEvaluator extends WalkNodeEvaluator {
      private static final int MAX_DISTANCE_TO_HOME_SQ = 1024;

      @Override
      public PathType getPathType(PathfindingContext p_377585_, int p_378027_, int p_378555_, int p_378341_) {
         BlockPos blockpos = Creaking.this.getHomePos();
         if (blockpos == null) {
            return super.getPathType(p_377585_, p_378027_, p_378555_, p_378341_);
         }

         double d0 = blockpos.distSqr(new Vec3i(p_378027_, p_378555_, p_378341_));
         return d0 > 1024.0 && d0 >= blockpos.distSqr(p_377585_.mobPosition())
            ? PathType.BLOCKED
            : super.getPathType(p_377585_, p_378027_, p_378555_, p_378341_);
      }
   }
}
