package net.minecraft.world.entity.animal.fish;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FollowFlockLeaderGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public abstract class AbstractSchoolingFish extends AbstractFish {
   private @Nullable AbstractSchoolingFish leader;
   private int schoolSize = 1;

   public AbstractSchoolingFish(final EntityType<? extends AbstractSchoolingFish> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(5, new FollowFlockLeaderGoal(this));
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return this.getMaxSchoolSize();
   }

   public int getMaxSchoolSize() {
      return super.getMaxSpawnClusterSize();
   }

   @Override
   protected boolean canRandomSwim() {
      return !this.isFollower();
   }

   public boolean isFollower() {
      return this.leader != null && this.leader.isAlive();
   }

   public AbstractSchoolingFish startFollowing(final AbstractSchoolingFish leader) {
      this.leader = leader;
      leader.addFollower();
      return leader;
   }

   public void stopFollowing() {
      this.leader.removeFollower();
      this.leader = null;
   }

   private void addFollower() {
      this.schoolSize++;
   }

   private void removeFollower() {
      this.schoolSize--;
   }

   public boolean canBeFollowed() {
      return this.hasFollowers() && this.schoolSize < this.getMaxSchoolSize();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.hasFollowers() && this.level().getRandom().nextInt(200) == 1) {
         List<? extends AbstractFish> neighbors = this.level()
            .getEntitiesOfClass((Class<? extends AbstractFish>)this.getClass(), this.getBoundingBox().inflate(8.0, 8.0, 8.0));
         if (neighbors.size() <= 1) {
            this.schoolSize = 1;
         }
      }
   }

   public boolean hasFollowers() {
      return this.schoolSize > 1;
   }

   public boolean inRangeOfLeader() {
      return this.distanceToSqr(this.leader) <= 121.0;
   }

   public void pathToLeader() {
      if (this.isFollower()) {
         this.getNavigation().moveTo(this.leader, 1.0);
      }
   }

   public void addFollowers(final Stream<? extends AbstractSchoolingFish> abstractSchoolingFishStream) {
      abstractSchoolingFishStream.limit(this.getMaxSchoolSize() - this.schoolSize).filter(f -> f != this).forEach(otherFish -> otherFish.startFollowing(this));
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      super.finalizeSpawn(level, difficulty, spawnReason, groupData);
      if (groupData == null) {
         groupData = new AbstractSchoolingFish.SchoolSpawnGroupData(this);
      } else {
         this.startFollowing(((AbstractSchoolingFish.SchoolSpawnGroupData)groupData).leader);
      }

      return groupData;
   }

   public static class SchoolSpawnGroupData implements SpawnGroupData {
      public final AbstractSchoolingFish leader;

      public SchoolSpawnGroupData(final AbstractSchoolingFish leader) {
         this.leader = leader;
      }
   }
}
