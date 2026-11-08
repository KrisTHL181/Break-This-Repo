package net.minecraft.world.waypoints;

public interface WaypointManager<T extends Waypoint> {
   void trackWaypoint(T var1);

   void updateWaypoint(T var1);

   void untrackWaypoint(T var1);
}
