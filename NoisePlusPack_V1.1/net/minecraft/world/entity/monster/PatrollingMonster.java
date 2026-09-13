package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class PatrollingMonster extends Monster {
   private static final boolean DEFAULT_PATROL_LEADER = false;
   private static final boolean DEFAULT_PATROLLING = false;
   private @Nullable BlockPos patrolTarget;
   private boolean patrolLeader = false;
   private boolean patrolling = false;

   protected PatrollingMonster(EntityType<? extends PatrollingMonster> p_33046_, Level p_33047_) {
      super(p_33046_, p_33047_);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(4, new PatrollingMonster.LongDistancePatrolGoal<>(this, 0.7, 0.595));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_410683_) {
      super.addAdditionalSaveData(p_410683_);
      p_410683_.storeNullable("patrol_target", BlockPos.CODEC, this.patrolTarget);
      p_410683_.putBoolean("PatrolLeader", this.patrolLeader);
      p_410683_.putBoolean("Patrolling", this.patrolling);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_410701_) {
      super.readAdditionalSaveData(p_410701_);
      this.patrolTarget = p_410701_.<BlockPos>read("patrol_target", BlockPos.CODEC).orElse(null);
      this.patrolLeader = p_410701_.getBooleanOr("PatrolLeader", false);
      this.patrolling = p_410701_.getBooleanOr("Patrolling", false);
   }

   public boolean canBeLeader() {
      return true;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_33049_, DifficultyInstance p_33050_, EntitySpawnReason p_370139_, @Nullable SpawnGroupData p_33052_
   ) {
      if (p_370139_ != EntitySpawnReason.PATROL
         && p_370139_ != EntitySpawnReason.EVENT
         && p_370139_ != EntitySpawnReason.STRUCTURE
         && p_33049_.getRandom().nextFloat() < 0.06F
         && this.canBeLeader()) {
         this.patrolLeader = true;
      }

      if (this.isPatrolLeader()) {
         this.setItemSlot(EquipmentSlot.HEAD, Raid.getOminousBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
         this.setDropChance(EquipmentSlot.HEAD, 2.0F);
      }

      if (p_370139_ == EntitySpawnReason.PATROL) {
         this.patrolling = true;
      }

      return super.finalizeSpawn(p_33049_, p_33050_, p_370139_, p_33052_);
   }

   public static boolean checkPatrollingMonsterSpawnRules(
      EntityType<? extends PatrollingMonster> p_219026_, LevelAccessor p_219027_, EntitySpawnReason p_363098_, BlockPos p_219029_, RandomSource p_219030_
   ) {
      return p_219027_.getBrightness(LightLayer.BLOCK, p_219029_) > 8
         ? false
         : checkAnyLightMonsterSpawnRules(p_219026_, p_219027_, p_363098_, p_219029_, p_219030_);
   }

   @Override
   public boolean removeWhenFarAway(double p_33073_) {
      return !this.patrolling || p_33073_ > 16384.0;
   }

   public void setPatrolTarget(BlockPos p_33071_) {
      this.patrolTarget = p_33071_;
      this.patrolling = true;
   }

   public @Nullable BlockPos getPatrolTarget() {
      return this.patrolTarget;
   }

   public boolean hasPatrolTarget() {
      return this.patrolTarget != null;
   }

   public void setPatrolLeader(boolean p_33076_) {
      this.patrolLeader = p_33076_;
      this.patrolling = true;
   }

   public boolean isPatrolLeader() {
      return this.patrolLeader;
   }

   public boolean canJoinPatrol() {
      return true;
   }

   public void findPatrolTarget() {
      this.patrolTarget = this.blockPosition().offset(-500 + this.random.nextInt(1000), 0, -500 + this.random.nextInt(1000));
      this.patrolling = true;
   }

   protected boolean isPatrolling() {
      return this.patrolling;
   }

   protected void setPatrolling(boolean p_33078_) {
      this.patrolling = p_33078_;
   }

   public static class LongDistancePatrolGoal<T extends PatrollingMonster> extends Goal {
      private static final int NAVIGATION_FAILED_COOLDOWN = 200;
      private final T mob;
      private final double speedModifier;
      private final double leaderSpeedModifier;
      private long cooldownUntil;

      public LongDistancePatrolGoal(T p_33084_, double p_33085_, double p_33086_) {
         this.mob = p_33084_;
         this.speedModifier = p_33085_;
         this.leaderSpeedModifier = p_33086_;
         this.cooldownUntil = -1L;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         boolean flag = this.mob.level().getGameTime() < this.cooldownUntil;
         return this.mob.isPatrolling() && this.mob.getTarget() == null && !this.mob.hasControllingPassenger() && this.mob.hasPatrolTarget() && !flag;
      }

      @Override
      public void start() {
      }

      @Override
      public void stop() {
      }

      @Override
      public void tick() {
         boolean flag = this.mob.isPatrolLeader();
         PathNavigation pathnavigation = this.mob.getNavigation();
         if (pathnavigation.isDone()) {
            List<PatrollingMonster> list = this.findPatrolCompanions();
            if (this.mob.isPatrolling() && list.isEmpty()) {
               this.mob.setPatrolling(false);
            } else if (flag && this.mob.getPatrolTarget().closerToCenterThan(this.mob.position(), 10.0)) {
               this.mob.findPatrolTarget();
            } else {
               Vec3 vec3 = Vec3.atBottomCenterOf(this.mob.getPatrolTarget());
               Vec3 vec31 = this.mob.position();
               Vec3 vec32 = vec31.subtract(vec3);
               vec3 = vec32.yRot(90.0F).scale(0.4).add(vec3);
               Vec3 vec33 = vec3.subtract(vec31).normalize().scale(10.0).add(vec31);
               BlockPos blockpos = BlockPos.containing(vec33);
               blockpos = this.mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockpos);
               if (!pathnavigation.moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), flag ? this.leaderSpeedModifier : this.speedModifier)) {
                  this.moveRandomly();
                  this.cooldownUntil = this.mob.level().getGameTime() + 200L;
               } else if (flag) {
                  for (PatrollingMonster patrollingmonster : list) {
                     patrollingmonster.setPatrolTarget(blockpos);
                  }
               }
            }
         }
      }

      private List<PatrollingMonster> findPatrolCompanions() {
         return this.mob
            .level()
            .getEntitiesOfClass(
               PatrollingMonster.class, this.mob.getBoundingBox().inflate(16.0), p_405506_ -> p_405506_.canJoinPatrol() && !p_405506_.is(this.mob)
            );
      }

      private boolean moveRandomly() {
         RandomSource randomsource = this.mob.getRandom();
         BlockPos blockpos = this.mob
            .level()
            .getHeightmapPos(
               Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.mob.blockPosition().offset(-8 + randomsource.nextInt(16), 0, -8 + randomsource.nextInt(16))
            );
         return this.mob.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);
      }
   }
}
