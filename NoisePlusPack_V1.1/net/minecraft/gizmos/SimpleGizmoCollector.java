package net.minecraft.gizmos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public class SimpleGizmoCollector implements GizmoCollector {
   private final List<SimpleGizmoCollector.GizmoInstance> gizmos = new ArrayList<>();
   private final List<SimpleGizmoCollector.GizmoInstance> temporaryGizmos = new ArrayList<>();

   @Override
   public GizmoProperties add(Gizmo p_456748_) {
      SimpleGizmoCollector.GizmoInstance simplegizmocollector$gizmoinstance = new SimpleGizmoCollector.GizmoInstance(p_456748_);
      this.gizmos.add(simplegizmocollector$gizmoinstance);
      return simplegizmocollector$gizmoinstance;
   }

   public List<SimpleGizmoCollector.GizmoInstance> drainGizmos() {
      ArrayList<SimpleGizmoCollector.GizmoInstance> arraylist = new ArrayList<>(this.gizmos);
      arraylist.addAll(this.temporaryGizmos);
      long i = Util.getMillis();
      this.gizmos.removeIf(p_456800_ -> p_456800_.getExpireTimeMillis() < i);
      this.temporaryGizmos.clear();
      return arraylist;
   }

   public List<SimpleGizmoCollector.GizmoInstance> getGizmos() {
      return this.gizmos;
   }

   public void addTemporaryGizmos(Collection<SimpleGizmoCollector.GizmoInstance> p_452760_) {
      this.temporaryGizmos.addAll(p_452760_);
   }

   public static class GizmoInstance implements GizmoProperties {
      private final Gizmo gizmo;
      private boolean isAlwaysOnTop;
      private long startTimeMillis;
      private long expireTimeMillis;
      private boolean shouldFadeOut;

      GizmoInstance(Gizmo p_455332_) {
         this.gizmo = p_455332_;
      }

      @Override
      public GizmoProperties setAlwaysOnTop() {
         this.isAlwaysOnTop = true;
         return this;
      }

      @Override
      public GizmoProperties persistForMillis(int p_452364_) {
         this.startTimeMillis = Util.getMillis();
         this.expireTimeMillis = this.startTimeMillis + p_452364_;
         return this;
      }

      @Override
      public GizmoProperties fadeOut() {
         this.shouldFadeOut = true;
         return this;
      }

      public float getAlphaMultiplier(long p_457774_) {
         if (this.shouldFadeOut) {
            long i = this.expireTimeMillis - this.startTimeMillis;
            long j = p_457774_ - this.startTimeMillis;
            return 1.0F - Mth.clamp((float)j / (float)i, 0.0F, 1.0F);
         } else {
            return 1.0F;
         }
      }

      public boolean isAlwaysOnTop() {
         return this.isAlwaysOnTop;
      }

      public long getExpireTimeMillis() {
         return this.expireTimeMillis;
      }

      public Gizmo gizmo() {
         return this.gizmo;
      }
   }
}
