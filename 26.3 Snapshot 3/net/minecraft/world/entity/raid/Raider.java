package net.minecraft.world.entity.raid;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PathfindToRaidGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class Raider extends PatrollingMonster {
   protected static final EntityDataAccessor<Boolean> IS_CELEBRATING = SynchedEntityData.defineId(Raider.class, EntityDataSerializers.BOOLEAN);
   private static final Predicate<ItemEntity> ALLOWED_ITEMS = e -> !e.hasPickUpDelay()
      && e.isAlive()
      && ItemStack.matches(e.getItem(), Raid.getOminousBannerInstance(e.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
   private static final int DEFAULT_WAVE = 0;
   private static final boolean DEFAULT_CAN_JOIN_RAID = false;
   protected @Nullable Raid raid;
   private int wave = 0;
   private boolean canJoinRaid = false;
   private int ticksOutsideRaid;

   protected Raider(final EntityType<? extends Raider> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(1, new Raider.ObtainRaidLeaderBannerGoal<>(this));
      this.goalSelector.addGoal(3, new PathfindToRaidGoal<>(this));
      this.goalSelector.addGoal(4, new Raider.RaiderMoveThroughVillageGoal(this, 1.05F, 1));
      this.goalSelector.addGoal(5, new Raider.RaiderCelebration(this));
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(IS_CELEBRATING, false);
   }

   public abstract void applyRaidBuffs(final ServerLevel level, final int wave, final boolean isCaptain);

   public boolean canJoinRaid() {
      return this.canJoinRaid;
   }

   public void setCanJoinRaid(final boolean canJoinRaid) {
      this.canJoinRaid = canJoinRaid;
   }

   @Override
   public void aiStep() {
      if (this.level() instanceof ServerLevel level && this.isAlive()) {
         Raid currentRaid = this.getCurrentRaid();
         if (this.canJoinRaid()) {
            if (currentRaid == null) {
               if (this.level().getGameTime() % 20L == 0L) {
                  Raid nearbyRaid = level.getRaidAt(this.blockPosition());
                  if (nearbyRaid != null && Raids.canJoinRaid(this)) {
                     nearbyRaid.joinRaid(level, nearbyRaid.getGroupsSpawned(), this, null, true);
                  }
               }
            } else {
               LivingEntity target = this.getTarget();
               if (target != null && (target.is(EntityTypes.PLAYER) || target.is(EntityTypes.IRON_GOLEM))) {
                  this.noActionTime = 0;
               }
            }
         }
      }

      super.aiStep();
   }

   @Override
   protected void updateNoActionTime() {
      this.noActionTime += 2;
   }

   @Override
   public void die(final DamageSource source) {
      if (this.level() instanceof ServerLevel serverLevel) {
         Entity killer = source.getEntity();
         Raid raidWhenKilled = this.getCurrentRaid();
         if (raidWhenKilled != null) {
            if (this.isPatrolLeader()) {
               raidWhenKilled.removeLeader(this.getWave());
            }

            if (killer != null && killer.is(EntityTypes.PLAYER)) {
               raidWhenKilled.addHeroOfTheVillage(killer);
            }

            raidWhenKilled.removeFromRaid(serverLevel, this, false);
         }
      }

      super.die(source);
   }

   @Override
   public boolean canJoinPatrol() {
      return !this.hasActiveRaid();
   }

   public void setCurrentRaid(final @Nullable Raid raid) {
      this.raid = raid;
   }

   public @Nullable Raid getCurrentRaid() {
      return this.raid;
   }

   public boolean isCaptain() {
      ItemStack banner = this.getItemBySlot(EquipmentSlot.HEAD);
      boolean hasCaptainBanner = !banner.isEmpty()
         && ItemStack.matches(banner, Raid.getOminousBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
      boolean patrolLeader = this.isPatrolLeader();
      return hasCaptainBanner && patrolLeader;
   }

   public boolean hasRaid() {
      return !(this.level() instanceof ServerLevel serverLevel) ? false : this.getCurrentRaid() != null || serverLevel.getRaidAt(this.blockPosition()) != null;
   }

   public boolean hasActiveRaid() {
      return this.getCurrentRaid() != null && this.getCurrentRaid().isActive();
   }

   public void setWave(final int wave) {
      this.wave = wave;
   }

   public int getWave() {
      return this.wave;
   }

   public boolean isCelebrating() {
      return this.entityData.get(IS_CELEBRATING);
   }

   public void setCelebrating(final boolean celebrating) {
      this.entityData.set(IS_CELEBRATING, celebrating);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("Wave", this.wave);
      output.putBoolean("CanJoinRaid", this.canJoinRaid);
      if (this.raid != null && this.level() instanceof ServerLevel level) {
         level.getRaids().getId(this.raid).ifPresent(id -> output.putInt("RaidId", id));
      }
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.wave = input.getIntOr("Wave", 0);
      this.canJoinRaid = input.getBooleanOr("CanJoinRaid", false);
      if (this.level() instanceof ServerLevel level) {
         input.getInt("RaidId").ifPresent(raidId -> {
            this.raid = level.getRaids().get(raidId);
            if (this.raid != null) {
               this.raid.addWaveMob(level, this.wave, this, false);
               if (this.isPatrolLeader()) {
                  this.raid.setLeader(this.wave, this);
               }
            }
         });
      }
   }

   @Override
   protected void pickUpItem(final ServerLevel level, final ItemEntity entity) {
      ItemStack itemStack = entity.getItem();
      boolean hasRaidLeader = this.hasActiveRaid() && this.getCurrentRaid().getLeader(this.getWave()) != null;
      if (this.hasActiveRaid()
         && !hasRaidLeader
         && ItemStack.matches(itemStack, Raid.getOminousBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)))) {
         EquipmentSlot slot = EquipmentSlot.HEAD;
         ItemStack current = this.getItemBySlot(slot);
         double dropChance = this.getDropChances().byEquipment(slot);
         if (!current.isEmpty() && Math.max(this.random.nextFloat() - 0.1F, 0.0F) < dropChance) {
            this.spawnAtLocation(level, current);
         }

         this.onItemPickup(entity);
         this.setItemSlot(slot, itemStack);
         this.take(entity, itemStack.getCount());
         entity.discard();
         this.getCurrentRaid().setLeader(this.getWave(), this);
         this.setPatrolLeader(true);
      } else {
         super.pickUpItem(level, entity);
      }
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return this.getCurrentRaid() == null ? super.removeWhenFarAway(distSqr) : false;
   }

   @Override
   public boolean requiresCustomPersistence() {
      return super.requiresCustomPersistence() || this.getCurrentRaid() != null;
   }

   public int getTicksOutsideRaid() {
      return this.ticksOutsideRaid;
   }

   public void setTicksOutsideRaid(final int ticksOutsideRaid) {
      this.ticksOutsideRaid = ticksOutsideRaid;
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      if (this.hasActiveRaid()) {
         this.getCurrentRaid().updateBossbar();
      }

      return super.hurtServer(level, source, damage);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      this.setCanJoinRaid(!this.is(EntityTypes.WITCH) || spawnReason != EntitySpawnReason.NATURAL);
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   public abstract SoundEvent getCelebrateSound();

   protected static class HoldGroundAttackGoal extends Goal {
      private final Raider mob;
      private final float hostileRadiusSqr;
      public final TargetingConditions shoutTargeting = TargetingConditions.forNonCombat().range(8.0).ignoreLineOfSight().ignoreInvisibilityTesting();

      public HoldGroundAttackGoal(final AbstractIllager mob, final float hostileRadius) {
         this.mob = mob;
         this.hostileRadiusSqr = hostileRadius * hostileRadius;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         LivingEntity lastHurtByMob = this.mob.getLastHurtByMob();
         return this.mob.getCurrentRaid() == null
            && this.mob.isPatrolling()
            && this.mob.getTarget() != null
            && !this.mob.isAggressive()
            && (lastHurtByMob == null || !lastHurtByMob.is(EntityTypes.PLAYER));
      }

      @Override
      public void start() {
         super.start();
         this.mob.getNavigation().stop();

         for (Raider entity : getServerLevel(this.mob)
            .getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0))) {
            entity.setTarget(this.mob.getTarget());
         }
      }

      @Override
      public void stop() {
         super.stop();
         LivingEntity target = this.mob.getTarget();
         if (target != null) {
            for (Raider entity : getServerLevel(this.mob)
               .getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0))) {
               entity.setTarget(target);
               entity.setAggressive(true);
            }

            this.mob.setAggressive(true);
         }
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         LivingEntity target = this.mob.getTarget();
         if (target != null) {
            if (this.mob.distanceToSqr(target) > this.hostileRadiusSqr) {
               this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
               if (this.mob.random.nextInt(50) == 0) {
                  this.mob.playAmbientSound();
               }
            } else {
               this.mob.setAggressive(true);
            }

            super.tick();
         }
      }
   }

   public class ObtainRaidLeaderBannerGoal<T extends Raider> extends Goal {
      private final T mob;
      private Int2LongOpenHashMap unreachableBannerCache = new Int2LongOpenHashMap();
      private @Nullable Path pathToBanner;
      private @Nullable ItemEntity pursuedBannerItemEntity;

      public ObtainRaidLeaderBannerGoal(final T mob) {
         this.mob = mob;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         if (this.cannotPickUpBanner()) {
            return false;
         }

         Int2LongOpenHashMap tempCache = new Int2LongOpenHashMap();
         double followRange = Raider.this.getAttributeValue(Attributes.FOLLOW_RANGE);

         for (ItemEntity banner : this.mob
            .level()
            .getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(followRange, 8.0, followRange), Raider.ALLOWED_ITEMS)) {
            long unreachableUntilTime = this.unreachableBannerCache.getOrDefault(banner.getId(), Long.MIN_VALUE);
            if (Raider.this.level().getGameTime() < unreachableUntilTime) {
               tempCache.put(banner.getId(), unreachableUntilTime);
            } else {
               Path path = this.mob.getNavigation().createPath(banner, 1);
               if (path != null && path.canReach()) {
                  this.pathToBanner = path;
                  this.pursuedBannerItemEntity = banner;
                  return true;
               }

               tempCache.put(banner.getId(), Raider.this.level().getGameTime() + 600L);
            }
         }

         this.unreachableBannerCache = tempCache;
         return false;
      }

      @Override
      public boolean canContinueToUse() {
         if (this.pursuedBannerItemEntity == null || this.pathToBanner == null) {
            return false;
         } else if (this.pursuedBannerItemEntity.isRemoved()) {
            return false;
         } else {
            return this.pathToBanner.isDone() ? false : !this.cannotPickUpBanner();
         }
      }

      private boolean cannotPickUpBanner() {
         if (!this.mob.hasActiveRaid()) {
            return true;
         }

         if (this.mob.getCurrentRaid().isOver()) {
            return true;
         }

         if (!this.mob.canBeLeader()) {
            return true;
         }

         if (ItemStack.matches(
            this.mob.getItemBySlot(EquipmentSlot.HEAD), Raid.getOminousBannerInstance(this.mob.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN))
         )) {
            return true;
         }

         Raider leader = Raider.this.raid.getLeader(this.mob.getWave());
         return leader != null && leader.isAlive();
      }

      @Override
      public void start() {
         this.mob.getNavigation().moveTo(this.pathToBanner, 1.15F);
      }

      @Override
      public void stop() {
         this.pathToBanner = null;
         this.pursuedBannerItemEntity = null;
      }

      @Override
      public void tick() {
         if (this.pursuedBannerItemEntity != null && this.pursuedBannerItemEntity.closerThan(this.mob, 1.414)) {
            this.mob.pickUpItem(getServerLevel(Raider.this.level()), this.pursuedBannerItemEntity);
         }
      }
   }

   public class RaiderCelebration extends Goal {
      private final Raider mob;

      public RaiderCelebration(final Raider mob) {
         this.mob = mob;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         Raid currentRaid = this.mob.getCurrentRaid();
         return this.mob.isAlive() && this.mob.getTarget() == null && currentRaid != null && currentRaid.isLoss();
      }

      @Override
      public void start() {
         this.mob.setCelebrating(true);
         super.start();
      }

      @Override
      public void stop() {
         this.mob.setCelebrating(false);
         super.stop();
      }

      @Override
      public void tick() {
         if (!this.mob.isSilent() && this.mob.random.nextInt(this.adjustedTickDelay(100)) == 0) {
            Raider.this.makeSound(Raider.this.getCelebrateSound());
         }

         if (!this.mob.isPassenger() && this.mob.random.nextInt(this.adjustedTickDelay(50)) == 0) {
            this.mob.getJumpControl().jump();
         }

         super.tick();
      }
   }

   private static class RaiderMoveThroughVillageGoal extends Goal {
      private final Raider raider;
      private final double speedModifier;
      private BlockPos poiPos;
      private final List<BlockPos> visited = Lists.newArrayList();
      private final int distanceToPoi;
      private boolean stuck;

      public RaiderMoveThroughVillageGoal(final Raider mob, final double speedModifier, final int distanceToPoi) {
         this.raider = mob;
         this.speedModifier = speedModifier;
         this.distanceToPoi = distanceToPoi;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         this.updateVisited();
         return this.isValidRaid() && this.hasSuitablePoi() && this.raider.getTarget() == null;
      }

      private boolean isValidRaid() {
         return this.raider.hasActiveRaid() && !this.raider.getCurrentRaid().isOver();
      }

      private boolean hasSuitablePoi() {
         ServerLevel level = (ServerLevel)this.raider.level();
         BlockPos pos = this.raider.blockPosition();
         Optional<BlockPos> homePos = level.getPoiManager()
            .getRandom(p -> p.is(PoiTypes.HOME), this::hasNotVisited, PoiManager.Occupancy.ANY, pos, 48, this.raider.random);
         if (homePos.isEmpty()) {
            return false;
         }

         this.poiPos = homePos.get().immutable();
         return true;
      }

      @Override
      public boolean canContinueToUse() {
         return this.raider.getNavigation().isDone()
            ? false
            : this.raider.getTarget() == null
               && !this.poiPos.closerToCenterThan(this.raider.position(), this.raider.getBbWidth() + this.distanceToPoi)
               && !this.stuck;
      }

      @Override
      public void stop() {
         if (this.poiPos.closerToCenterThan(this.raider.position(), this.distanceToPoi)) {
            this.visited.add(this.poiPos);
         }
      }

      @Override
      public void start() {
         super.start();
         this.raider.setNoActionTime(0);
         this.raider.getNavigation().moveTo(this.poiPos.getX(), this.poiPos.getY(), this.poiPos.getZ(), this.speedModifier);
         this.stuck = false;
      }

      @Override
      public void tick() {
         if (this.raider.getNavigation().isDone()) {
            Vec3 poiVec = Vec3.atBottomCenterOf(this.poiPos);
            Vec3 nextPos = DefaultRandomPos.getPosTowards(this.raider, 16, 7, poiVec, (float) (Math.PI / 10));
            if (nextPos == null) {
               nextPos = DefaultRandomPos.getPosTowards(this.raider, 8, 7, poiVec, (float) (Math.PI / 2));
            }

            if (nextPos == null) {
               this.stuck = true;
               return;
            }

            this.raider.getNavigation().moveTo(nextPos.x, nextPos.y, nextPos.z, this.speedModifier);
         }
      }

      private boolean hasNotVisited(final BlockPos poi) {
         for (BlockPos visitedPoi : this.visited) {
            if (Objects.equals(poi, visitedPoi)) {
               return false;
            }
         }

         return true;
      }

      private void updateVisited() {
         if (this.visited.size() > 2) {
            this.visited.remove(0);
         }
      }
   }
}
