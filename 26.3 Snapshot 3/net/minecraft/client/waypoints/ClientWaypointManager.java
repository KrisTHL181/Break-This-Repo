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

public class ClientWaypointManager implements TrackedWaypointManager {
   private final Map<Either<UUID, String>, TrackedWaypoint> waypoints = new ConcurrentHashMap<>();

   public void trackWaypoint(final TrackedWaypoint waypoint) {
      this.waypoints.put(waypoint.id(), waypoint);
   }

   public void updateWaypoint(final TrackedWaypoint waypoint) {
      TrackedWaypoint trackedWaypoint = this.waypoints.get(waypoint.id());
      if (trackedWaypoint != null) {
         trackedWaypoint.update(waypoint);
      }
   }

   public void untrackWaypoint(final TrackedWaypoint waypoint) {
      this.waypoints.remove(waypoint.id());
   }

   public boolean hasWaypoints() {
      return !this.waypoints.isEmpty();
   }

   public void forEachWaypoint(final Entity fromEntity, final Consumer<TrackedWaypoint> consumer) {
      this.waypoints
         .values()
         .stream()
         .sorted(Comparator.<TrackedWaypoint>comparingDouble(waypoint -> waypoint.distanceSquared(fromEntity)).reversed())
         .forEachOrdered(consumer);
   }
}
