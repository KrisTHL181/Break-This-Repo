package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EnderMan extends Monster implements NeutralMob {
   private static final Identifier SPEED_MODIFIER_ATTACKING_ID = Identifier.withDefaultNamespace("attacking");
   private static final AttributeModifier SPEED_MODIFIER_ATTACKING = new AttributeModifier(
      SPEED_MODIFIER_ATTACKING_ID, 0.15F, AttributeModifier.Operation.ADD_VALUE
   );
   private static final int DELAY_BETWEEN_CREEPY_STARE_SOUND = 400;
   private static final int MIN_DEAGGRESSION_TIME = 600;
   private static final EntityDataAccessor<Optional<BlockState>> DATA_CARRY_STATE = SynchedEntityData.defineId(
      EnderMan.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE
   );
   private static final EntityDataAccessor<Boolean> DATA_CREEPY = SynchedEntityData.defineId(EnderMan.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_STARED_AT = SynchedEntityData.defineId(EnderMan.class, EntityDataSerializers.BOOLEAN);
   private int lastStareSound = Integer.MIN_VALUE;
   private int targetChangeTime;
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   private long persistentAngerEndTime;
   private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

   public EnderMan(EntityType<? extends EnderMan> p_32485_, Level p_32486_) {
      super(p_32485_, p_32486_);
      this.setPathfindingMalus(PathType.WATER, -1.0F);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new EnderMan.EndermanFreezeWhenLookedAt(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.goalSelector.addGoal(10, new EnderMan.EndermanLeaveBlockGoal(this));
      this.goalSelector.addGoal(11, new EnderMan.EndermanTakeBlockGoal(this));
      this.targetSelector.addGoal(1, new EnderMan.EndermanLookForPlayerGoal(this, this::isAngryAt));
      this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Endermite.class, true, false));
      this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
   }

   @Override
   public float getWalkTargetValue(BlockPos p_428154_, LevelReader p_430168_) {
      return 0.0F;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 40.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3F)
         .add(Attributes.ATTACK_DAMAGE, 7.0)
         .add(Attributes.FOLLOW_RANGE, 64.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   public void setTarget(@Nullable LivingEntity p_32537_) {
      super.setTarget(p_32537_);
      AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (p_32537_ == null) {
         this.targetChangeTime = 0;
         this.entityData.set(DATA_CREEPY, false);
         this.entityData.set(DATA_STARED_AT, false);
         attributeinstance.removeModifier(SPEED_MODIFIER_ATTACKING_ID);
      } else {
         this.targetChangeTime = this.tickCount;
         this.entityData.set(DATA_CREEPY, true);
         if (!attributeinstance.hasModifier(SPEED_MODIFIER_ATTACKING_ID)) {
            attributeinstance.addTransientModifier(SPEED_MODIFIER_ATTACKING);
         }
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_328864_) {
      super.defineSynchedData(p_328864_);
      p_328864_.define(DATA_CARRY_STATE, Optional.empty());
      p_328864_.define(DATA_CREEPY, false);
      p_328864_.define(DATA_STARED_AT, false);
   }

   @Override
   public void startPersistentAngerTimer() {
      this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   @Override
   public void setPersistentAngerEndTime(long p_452922_) {
      this.persistentAngerEndTime = p_452922_;
   }

   @Override
   public long getPersistentAngerEndTime() {
      return this.persistentAngerEndTime;
   }

   @Override
   public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> p_451821_) {
      this.persistentAngerTarget = p_451821_;
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   public void playStareSound() {
      if (this.tickCount >= this.lastStareSound + 400) {
         this.lastStareSound = this.tickCount;
         if (!this.isSilent()) {
            this.level().playLocalSound(this.getX(), this.getEyeY(), this.getZ(), SoundEvents.ENDERMAN_STARE, this.getSoundSource(), 2.5F, 1.0F, false);
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_32513_) {
      if (DATA_CREEPY.equals(p_32513_) && this.hasBeenStaredAt() && this.level().isClientSide()) {
         this.playStareSound();
      }

      super.onSyncedDataUpdated(p_32513_);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_409686_) {
      super.addAdditionalSaveData(p_409686_);
      BlockState blockstate = this.getCarriedBlock();
      if (blockstate != null) {
         p_409686_.store("carriedBlockState", BlockState.CODEC, blockstate);
      }

      this.addPersistentAngerSaveData(p_409686_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406341_) {
      super.readAdditionalSaveData(p_406341_);
      this.setCarriedBlock(p_406341_.<BlockState>read("carriedBlockState", BlockState.CODEC).filter(p_393340_ -> !p_393340_.isAir()).orElse(null));
      this.readPersistentAngerSaveData(this.level(), p_406341_);
   }

   boolean isBeingStaredBy(Player p_368759_) {
      return !LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM.test(p_368759_) ? false : this.isLookingAtMe(p_368759_, 0.025, true, false, this.getEyeY());
   }

   @Override
   public void aiStep() {
      if (this.level().isClientSide()) {
         for (int i = 0; i < 2; i++) {
            this.level()
               .addParticle(
                  ParticleTypes.PORTAL,
                  this.getRandomX(0.5),
                  this.getRandomY() - 0.25,
                  this.getRandomZ(0.5),
                  (this.random.nextDouble() - 0.5) * 2.0,
                  -this.random.nextDouble(),
                  (this.random.nextDouble() - 0.5) * 2.0
               );
         }
      }

      this.jumping = false;
      if (!this.level().isClientSide()) {
         this.updatePersistentAnger((ServerLevel)this.level(), true);
      }

      super.aiStep();
   }

   @Override
   public boolean isSensitiveToWater() {
      return true;
   }

   @Override
   protected void customServerAiStep(ServerLevel p_369687_) {
      if (p_369687_.isBrightOutside() && this.tickCount >= this.targetChangeTime + 600) {
         float f = this.getLightLevelDependentMagicValue();
         if (f > 0.5F && p_369687_.canSeeSky(this.blockPosition()) && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F) {
            this.setTarget(null);
            this.teleport();
         }
      }

      super.customServerAiStep(p_369687_);
   }

   protected boolean teleport() {
      if (!this.level().isClientSide() && this.isAlive()) {
         double d0 = this.getX() + (this.random.nextDouble() - 0.5) * 64.0;
         double d1 = this.getY() + (this.random.nextInt(64) - 32);
         double d2 = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
         return this.teleport(d0, d1, d2);
      } else {
         return false;
      }
   }

   boolean teleportTowards(Entity p_32501_) {
      Vec3 vec3 = new Vec3(this.getX() - p_32501_.getX(), this.getY(0.5) - p_32501_.getEyeY(), this.getZ() - p_32501_.getZ());
      vec3 = vec3.normalize();
      double d0 = 16.0;
      double d1 = this.getX() + (this.random.nextDouble() - 0.5) * 8.0 - vec3.x * 16.0;
      double d2 = this.getY() + (this.random.nextInt(16) - 8) - vec3.y * 16.0;
      double d3 = this.getZ() + (this.random.nextDouble() - 0.5) * 8.0 - vec3.z * 16.0;
      return this.teleport(d1, d2, d3);
   }

   private boolean teleport(double p_32544_, double p_32545_, double p_32546_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(p_32544_, p_32545_, p_32546_);

      while (blockpos$mutableblockpos.getY() > this.level().getMinY() && !this.level().getBlockState(blockpos$mutableblockpos).blocksMotion()) {
         blockpos$mutableblockpos.move(Direction.DOWN);
      }

      BlockState blockstate = this.level().getBlockState(blockpos$mutableblockpos);
      boolean flag = blockstate.blocksMotion();
      boolean flag1 = blockstate.getFluidState().is(FluidTags.WATER);
      if (flag && !flag1) {
         Vec3 vec3 = this.position();
         boolean flag2 = this.randomTeleport(p_32544_, p_32545_, p_32546_, true);
         if (flag2) {
            this.level().gameEvent(GameEvent.TELEPORT, vec3, GameEvent.Context.of(this));
            if (!this.isSilent()) {
               this.level().playSound(null, this.xo, this.yo, this.zo, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
               this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            }
         }

         return flag2;
      } else {
         return false;
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isCreepy() ? SoundEvents.ENDERMAN_SCREAM : SoundEvents.ENDERMAN_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_32527_) {
      return SoundEvents.ENDERMAN_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.ENDERMAN_DEATH;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel p_344632_, DamageSource p_32497_, boolean p_32499_) {
      super.dropCustomDeathLoot(p_344632_, p_32497_, p_32499_);
      BlockState blockstate = this.getCarriedBlock();
      if (blockstate != null) {
         ItemStack itemstack = new ItemStack(Items.DIAMOND_AXE);
         EnchantmentHelper.enchantItemFromProvider(
            itemstack,
            p_344632_.registryAccess(),
            VanillaEnchantmentProviders.ENDERMAN_LOOT_DROP,
            p_344632_.getCurrentDifficultyAt(this.blockPosition()),
            this.getRandom()
         );
         LootParams.Builder lootparams$builder = new LootParams.Builder((ServerLevel)this.level())
            .withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.TOOL, itemstack)
            .withOptionalParameter(LootContextParams.THIS_ENTITY, this);

         for (ItemStack itemstack1 : blockstate.getDrops(lootparams$builder)) {
            this.spawnAtLocation(p_344632_, itemstack1);
         }
      }
   }

   public void setCarriedBlock(@Nullable BlockState p_32522_) {
      this.entityData.set(DATA_CARRY_STATE, Optional.ofNullable(p_32522_));
   }

   public @Nullable BlockState getCarriedBlock() {
      return this.entityData.get(DATA_CARRY_STATE).orElse(null);
   }

   @Override
   public boolean hurtServer(ServerLevel p_362685_, DamageSource p_361014_, float p_368121_) {
      if (this.isInvulnerableTo(p_362685_, p_361014_)) {
         return false;
      }

      AbstractThrownPotion abstractthrownpotion = p_361014_.getDirectEntity() instanceof AbstractThrownPotion abstractthrownpotion1
         ? abstractthrownpotion1
         : null;
      if (!p_361014_.is(DamageTypeTags.IS_PROJECTILE) && abstractthrownpotion == null) {
         boolean flag1 = super.hurtServer(p_362685_, p_361014_, p_368121_);
         if (!(p_361014_.getEntity() instanceof LivingEntity) && this.random.nextInt(10) != 0) {
            this.teleport();
         }

         return flag1;
      } else {
         boolean flag = abstractthrownpotion != null && this.hurtWithCleanWater(p_362685_, p_361014_, abstractthrownpotion, p_368121_);

         for (int i = 0; i < 64; i++) {
            if (this.teleport()) {
               return true;
            }
         }

         return flag;
      }
   }

   private boolean hurtWithCleanWater(ServerLevel p_362708_, DamageSource p_186273_, AbstractThrownPotion p_460384_, float p_186275_) {
      ItemStack itemstack = p_460384_.getItem();
      PotionContents potioncontents = itemstack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      return potioncontents.is(Potions.WATER) ? super.hurtServer(p_362708_, p_186273_, p_186275_) : false;
   }

   public boolean isCreepy() {
      return this.entityData.get(DATA_CREEPY);
   }

   public boolean hasBeenStaredAt() {
      return this.entityData.get(DATA_STARED_AT);
   }

   public void setBeingStaredAt() {
      this.entityData.set(DATA_STARED_AT, true);
   }

   @Override
   public boolean requiresCustomPersistence() {
      return super.requiresCustomPersistence() || this.getCarriedBlock() != null;
   }

   static class EndermanFreezeWhenLookedAt extends Goal {
      private final EnderMan enderman;
      private @Nullable LivingEntity target;

      public EndermanFreezeWhenLookedAt(EnderMan p_32550_) {
         this.enderman = p_32550_;
         this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         this.target = this.enderman.getTarget();
         if (this.target instanceof Player player) {
            double d0 = this.target.distanceToSqr(this.enderman);
            return d0 > 256.0 ? false : this.enderman.isBeingStaredBy(player);
         } else {
            return false;
         }
      }

      @Override
      public void start() {
         this.enderman.getNavigation().stop();
      }

      @Override
      public void tick() {
         this.enderman.getLookControl().setLookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
      }
   }

   static class EndermanLeaveBlockGoal extends Goal {
      private final EnderMan enderman;

      public EndermanLeaveBlockGoal(EnderMan p_32556_) {
         this.enderman = p_32556_;
      }

      @Override
      public boolean canUse() {
         if (this.enderman.getCarriedBlock() == null) {
            return false;
         } else {
            return !getServerLevel(this.enderman).getGameRules().get(GameRules.MOB_GRIEFING)
               ? false
               : this.enderman.getRandom().nextInt(reducedTickDelay(2000)) == 0;
         }
      }

      @Override
      public void tick() {
         RandomSource randomsource = this.enderman.getRandom();
         Level level = this.enderman.level();
         int i = Mth.floor(this.enderman.getX() - 1.0 + randomsource.nextDouble() * 2.0);
         int j = Mth.floor(this.enderman.getY() + randomsource.nextDouble() * 2.0);
         int k = Mth.floor(this.enderman.getZ() - 1.0 + randomsource.nextDouble() * 2.0);
         BlockPos blockpos = new BlockPos(i, j, k);
         BlockState blockstate = level.getBlockState(blockpos);
         BlockPos blockpos1 = blockpos.below();
         BlockState blockstate1 = level.getBlockState(blockpos1);
         BlockState blockstate2 = this.enderman.getCarriedBlock();
         if (blockstate2 != null) {
            blockstate2 = Block.updateFromNeighbourShapes(blockstate2, this.enderman.level(), blockpos);
            if (this.canPlaceBlock(level, blockpos, blockstate2, blockstate, blockstate1, blockpos1)) {
               level.setBlock(blockpos, blockstate2, 3);
               level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(this.enderman, blockstate2));
               this.enderman.setCarriedBlock(null);
            }
         }
      }

      private boolean canPlaceBlock(Level p_32559_, BlockPos p_32560_, BlockState p_32561_, BlockState p_32562_, BlockState p_32563_, BlockPos p_32564_) {
         return p_32562_.isAir()
            && !p_32563_.isAir()
            && !p_32563_.is(Blocks.BEDROCK)
            && p_32563_.isCollisionShapeFullBlock(p_32559_, p_32564_)
            && p_32561_.canSurvive(p_32559_, p_32560_)
            && p_32559_.getEntities(this.enderman, AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(p_32560_))).isEmpty();
      }
   }

   static class EndermanLookForPlayerGoal extends NearestAttackableTargetGoal<Player> {
      private final EnderMan enderman;
      private @Nullable Player pendingTarget;
      private int aggroTime;
      private int teleportTime;
      private final TargetingConditions startAggroTargetConditions;
      private final TargetingConditions continueAggroTargetConditions = TargetingConditions.forCombat().ignoreLineOfSight();
      private final TargetingConditions.Selector isAngerInducing;

      public EndermanLookForPlayerGoal(EnderMan p_32573_, TargetingConditions.@Nullable Selector p_369637_) {
         super(p_32573_, Player.class, 10, false, false, p_369637_);
         this.enderman = p_32573_;
         this.isAngerInducing = (p_405502_, p_405503_) -> (p_32573_.isBeingStaredBy((Player)p_405502_) || p_32573_.isAngryAt(p_405502_, p_405503_))
            && !p_32573_.hasIndirectPassenger(p_405502_);
         this.startAggroTargetConditions = TargetingConditions.forCombat().range(this.getFollowDistance()).selector(this.isAngerInducing);
      }

      @Override
      public boolean canUse() {
         this.pendingTarget = getServerLevel(this.enderman).getNearestPlayer(this.startAggroTargetConditions.range(this.getFollowDistance()), this.enderman);
         return this.pendingTarget != null;
      }

      @Override
      public void start() {
         this.aggroTime = this.adjustedTickDelay(5);
         this.teleportTime = 0;
         this.enderman.setBeingStaredAt();
      }

      @Override
      public void stop() {
         this.pendingTarget = null;
         super.stop();
      }

      @Override
      public boolean canContinueToUse() {
         if (this.pendingTarget != null) {
            if (!this.isAngerInducing.test(this.pendingTarget, getServerLevel(this.enderman))) {
               return false;
            }

            this.enderman.lookAt(this.pendingTarget, 10.0F, 10.0F);
            return true;
         } else {
            if (this.target != null) {
               if (this.enderman.hasIndirectPassenger(this.target)) {
                  return false;
               }

               if (this.continueAggroTargetConditions.test(getServerLevel(this.enderman), this.enderman, this.target)) {
                  return true;
               }
            }

            return super.canContinueToUse();
         }
      }

      @Override
      public void tick() {
         if (this.enderman.getTarget() == null) {
            super.setTarget(null);
         }

         if (this.pendingTarget != null) {
            if (--this.aggroTime <= 0) {
               this.target = this.pendingTarget;
               this.pendingTarget = null;
               super.start();
            }
         } else {
            if (this.target != null && !this.enderman.isPassenger()) {
               if (this.enderman.isBeingStaredBy((Player)this.target)) {
                  if (this.target.distanceToSqr(this.enderman) < 16.0) {
                     this.enderman.teleport();
                  }

                  this.teleportTime = 0;
               } else if (this.target.distanceToSqr(this.enderman) > 256.0
                  && this.teleportTime++ >= this.adjustedTickDelay(30)
                  && this.enderman.teleportTowards(this.target)) {
                  this.teleportTime = 0;
               }
            }

            super.tick();
         }
      }
   }

   static class EndermanTakeBlockGoal extends Goal {
      private final EnderMan enderman;

      public EndermanTakeBlockGoal(EnderMan p_32585_) {
         this.enderman = p_32585_;
      }

      @Override
      public boolean canUse() {
         if (this.enderman.getCarriedBlock() != null) {
            return false;
         } else {
            return !getServerLevel(this.enderman).getGameRules().get(GameRules.MOB_GRIEFING)
               ? false
               : this.enderman.getRandom().nextInt(reducedTickDelay(20)) == 0;
         }
      }

      @Override
      public void tick() {
         RandomSource randomsource = this.enderman.getRandom();
         Level level = this.enderman.level();
         int i = Mth.floor(this.enderman.getX() - 2.0 + randomsource.nextDouble() * 4.0);
         int j = Mth.floor(this.enderman.getY() + randomsource.nextDouble() * 3.0);
         int k = Mth.floor(this.enderman.getZ() - 2.0 + randomsource.nextDouble() * 4.0);
         BlockPos blockpos = new BlockPos(i, j, k);
         BlockState blockstate = level.getBlockState(blockpos);
         Vec3 vec3 = new Vec3(this.enderman.getBlockX() + 0.5, j + 0.5, this.enderman.getBlockZ() + 0.5);
         Vec3 vec31 = new Vec3(i + 0.5, j + 0.5, k + 0.5);
         BlockHitResult blockhitresult = level.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.enderman));
         boolean flag = blockhitresult.getBlockPos().equals(blockpos);
         if (blockstate.is(BlockTags.ENDERMAN_HOLDABLE) && flag) {
            level.removeBlock(blockpos, false);
            level.gameEvent(GameEvent.BLOCK_DESTROY, blockpos, GameEvent.Context.of(this.enderman, blockstate));
            this.enderman.setCarriedBlock(blockstate.getBlock().defaultBlockState());
         }
      }
   }
}
