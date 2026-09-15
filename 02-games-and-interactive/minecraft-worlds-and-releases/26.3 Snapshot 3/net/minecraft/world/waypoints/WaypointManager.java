package net.minecraft.world.waypoints;

public interface WaypointManager<T extends Waypoint> {
   void trackWaypoint(T waypoint);

   void updateWaypoint(T waypoint);

   void untrackWaypoint(T waypoint);
}
