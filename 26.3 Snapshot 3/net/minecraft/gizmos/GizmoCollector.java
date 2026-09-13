package net.minecraft.gizmos;

public interface GizmoCollector {
   GizmoProperties IGNORED = new GizmoProperties() {
      @Override
      public GizmoProperties setAlwaysOnTop() {
         return this;
      }

      @Override
      public GizmoProperties persistForMillis(final int milliseconds) {
         return this;
      }

      @Override
      public GizmoProperties fadeOut() {
         return this;
      }
   };
   GizmoCollector NOOP = gizmo -> IGNORED;

   GizmoProperties add(final Gizmo gizmo);
}
