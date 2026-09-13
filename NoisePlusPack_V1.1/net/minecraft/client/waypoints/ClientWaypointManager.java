package net.minecraft.client.waypoints;

import com.mojang.datafixers.util.Either;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.TrackedWaypointManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientWaypointManager implements TrackedWaypointManager {
   private final Map<Either<UUID, String>, TrackedWaypoint> waypoints = new ConcurrentHashMap<>();

   public void trackWaypoint(TrackedWaypoint p_408162_) {
      this.waypoints.put(p_408162_.id(), p_408162_);
   }

   public void updateWaypoint(TrackedWaypoint p_410495_) {
      this.waypoints.get(p_410495_.id()).update(p_410495_);
   }

   public void untrackWaypoint(TrackedWaypoint p_409941_) {
      this.waypoints.remove(p_409941_.id());
   }

   public boolean hasWaypoints() {
      return !this.waypoints.isEmpty();
   }

   public void forEachWaypoint(Entity p_407298_, Consumer<TrackedWaypoint> p_407539_) {
      this.waypoints
         .values()
         .stream()
         .sorted(Comparator.<TrackedWaypoint>comparingDouble(p_410125_ -> p_410125_.distanceSquared(p_407298_)).reversed())
         .forEachOrdered(p_407539_);
   }
}
