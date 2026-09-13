package net.minecraft.world.entity.ai.goal;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MoveThroughVillageGoal extends Goal {
   protected final PathfinderMob mob;
   private final double speedModifier;
   private @Nullable Path path;
   private BlockPos poiPos;
   private final boolean onlyAtNight;
   private final List<BlockPos> visited = Lists.newArrayList();
   private final int distanceToPoi;
   private final BooleanSupplier canDealWithDoors;

   public MoveThroughVillageGoal(PathfinderMob p_25582_, double p_25583_, boolean p_25584_, int p_25585_, BooleanSupplier p_25586_) {
      this.mob = p_25582_;
      this.speedModifier = p_25583_;
      this.onlyAtNight = p_25584_;
      this.distanceToPoi = p_25585_;
      this.canDealWithDoors = p_25586_;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      if (!GoalUtils.hasGroundPathNavigation(p_25582_)) {
         throw new IllegalArgumentException("Unsupported mob for MoveThroughVillageGoal");
      }
   }

   @Override
   public boolean canUse() {
      if (!GoalUtils.hasGroundPathNavigation(this.mob)) {
         return false;
      }

      this.updateVisited();
      if (this.onlyAtNight && this.mob.level().isBrightOutside()) {
         return false;
      }

      ServerLevel serverlevel = (ServerLevel)this.mob.level();
      BlockPos blockpos = this.mob.blockPosition();
      if (!serverlevel.isCloseToVillage(blockpos, 6)) {
         return false;
      }

      Vec3 vec3 = LandRandomPos.getPos(
         this.mob,
         15,
         7,
         p_217751_ -> {
            if (!serverlevel.isVillage(p_217751_)) {
               return Double.NEGATIVE_INFINITY;
            }

            Optional<BlockPos> optional1 = serverlevel.getPoiManager()
               .find(p_217758_ -> p_217758_.is(PoiTypeTags.VILLAGE), this::hasNotVisited, p_217751_, 10, PoiManager.Occupancy.IS_OCCUPIED);
            return optional1.<Double>map(p_217754_ -> -p_217754_.distSqr(blockpos)).orElse(Double.NEGATIVE_INFINITY);
         }
      );
      if (vec3 == null) {
         return false;
      }

      Optional<BlockPos> optional = serverlevel.getPoiManager()
         .find(p_217756_ -> p_217756_.is(PoiTypeTags.VILLAGE), this::hasNotVisited, BlockPos.containing(vec3), 10, PoiManager.Occupancy.IS_OCCUPIED);
      if (optional.isEmpty()) {
         return false;
      }

      this.poiPos = optional.get().immutable();
      PathNavigation pathnavigation = this.mob.getNavigation();
      pathnavigation.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
      this.path = pathnavigation.createPath(this.poiPos, 0);
      pathnavigation.setCanOpenDoors(true);
      if (this.path == null) {
         Vec3 vec31 = DefaultRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf(this.poiPos), (float) (Math.PI / 2));
         if (vec31 == null) {
            return false;
         }

         pathnavigation.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
         this.path = this.mob.getNavigation().createPath(vec31.x, vec31.y, vec31.z, 0);
         pathnavigation.setCanOpenDoors(true);
         if (this.path == null) {
            return false;
         }
      }

      for (int i = 0; i < this.path.getNodeCount(); i++) {
         Node node = this.path.getNode(i);
         BlockPos blockpos1 = new BlockPos(node.x, node.y + 1, node.z);
         if (DoorBlock.isWoodenDoor(this.mob.level(), blockpos1)) {
            this.path = this.mob.getNavigation().createPath(node.x, node.y, node.z, 0);
            break;
         }
      }

      return this.path != null;
   }

   @Override
   public boolean canContinueToUse() {
      return this.mob.getNavigation().isDone() ? false : !this.poiPos.closerToCenterThan(this.mob.position(), this.mob.getBbWidth() + this.distanceToPoi);
   }

   @Override
   public void start() {
      this.mob.getNavigation().moveTo(this.path, this.speedModifier);
   }

   @Override
   public void stop() {
      if (this.mob.getNavigation().isDone() || this.poiPos.closerToCenterThan(this.mob.position(), this.distanceToPoi)) {
         this.visited.add(this.poiPos);
      }
   }

   private boolean hasNotVisited(BlockPos p_25593_) {
      for (BlockPos blockpos : this.visited) {
         if (Objects.equals(p_25593_, blockpos)) {
            return false;
         }
      }

      return true;
   }

   private void updateVisited() {
      if (this.visited.size() > 15) {
         this.visited.remove(0);
      }
   }
}
