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

   public AbstractSchoolingFish(EntityType<? extends AbstractSchoolingFish> p_451362_, Level p_460849_) {
      super(p_451362_, p_460849_);
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

   public AbstractSchoolingFish startFollowing(AbstractSchoolingFish p_456314_) {
      this.leader = p_456314_;
      p_456314_.addFollower();
      return p_456314_;
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
      if (this.hasFollowers() && this.level().random.nextInt(200) == 1) {
         List<? extends AbstractFish> list = this.level()
            .getEntitiesOfClass((Class<? extends AbstractFish>)this.getClass(), this.getBoundingBox().inflate(8.0, 8.0, 8.0));
         if (list.size() <= 1) {
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

   public void addFollowers(Stream<? extends AbstractSchoolingFish> p_454221_) {
      p_454221_.limit(this.getMaxSchoolSize() - this.schoolSize).filter(p_456074_ -> p_456074_ != this).forEach(p_455573_ -> p_455573_.startFollowing(this));
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_459930_, DifficultyInstance p_451364_, EntitySpawnReason p_457602_, @Nullable SpawnGroupData p_451076_
   ) {
      super.finalizeSpawn(p_459930_, p_451364_, p_457602_, p_451076_);
      if (p_451076_ == null) {
         p_451076_ = new AbstractSchoolingFish.SchoolSpawnGroupData(this);
      } else {
         this.startFollowing(((AbstractSchoolingFish.SchoolSpawnGroupData)p_451076_).leader);
      }

      return p_451076_;
   }

   public static class SchoolSpawnGroupData implements SpawnGroupData {
      public final AbstractSchoolingFish leader;

      public SchoolSpawnGroupData(AbstractSchoolingFish p_454082_) {
         this.leader = p_454082_;
      }
   }
}
