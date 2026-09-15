package net.minecraft.gizmos;

public interface GizmoProperties {
   GizmoProperties setAlwaysOnTop();

   GizmoProperties persistForMillis(int milliseconds);

   GizmoProperties fadeOut();
}
