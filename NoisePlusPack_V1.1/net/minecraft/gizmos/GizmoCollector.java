package net.minecraft.gizmos;

public interface GizmoCollector {
   GizmoProperties IGNORED = new GizmoProperties() {
      @Override
      public GizmoProperties setAlwaysOnTop() {
         return this;
      }

      @Override
      public GizmoProperties persistForMillis(int p_459235_) {
         return this;
      }

      @Override
      public GizmoProperties fadeOut() {
         return this;
      }
   };
   GizmoCollector NOOP = p_452891_ -> IGNORED;

   GizmoProperties add(Gizmo var1);
}
