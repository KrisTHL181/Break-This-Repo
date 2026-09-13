package net.minecraft.gizmos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Gizmos {
   static final ThreadLocal<@Nullable GizmoCollector> collector = new ThreadLocal<>();

   private Gizmos() {
   }

   public static Gizmos.TemporaryCollection withCollector(GizmoCollector p_454878_) {
      Gizmos.TemporaryCollection gizmos$temporarycollection = new Gizmos.TemporaryCollection();
      collector.set(p_454878_);
      return gizmos$temporarycollection;
   }

   public static GizmoProperties addGizmo(Gizmo p_457177_) {
      GizmoCollector gizmocollector = collector.get();
      if (gizmocollector == null) {
         throw new IllegalStateException("Gizmos cannot be created here! No GizmoCollector has been registered.");
      } else {
         return gizmocollector.add(p_457177_);
      }
   }

   public static GizmoProperties cuboid(AABB p_451857_, GizmoStyle p_460588_) {
      return cuboid(p_451857_, p_460588_, false);
   }

   public static GizmoProperties cuboid(AABB p_457172_, GizmoStyle p_460845_, boolean p_457779_) {
      return addGizmo(new CuboidGizmo(p_457172_, p_460845_, p_457779_));
   }

   public static GizmoProperties cuboid(BlockPos p_456071_, GizmoStyle p_459276_) {
      return cuboid(new AABB(p_456071_), p_459276_);
   }

   public static GizmoProperties cuboid(BlockPos p_455687_, float p_451188_, GizmoStyle p_454073_) {
      return cuboid(new AABB(p_455687_).inflate(p_451188_), p_454073_);
   }

   public static GizmoProperties circle(Vec3 p_459325_, float p_452362_, GizmoStyle p_450168_) {
      return addGizmo(new CircleGizmo(p_459325_, p_452362_, p_450168_));
   }

   public static GizmoProperties line(Vec3 p_453752_, Vec3 p_451524_, int p_456604_) {
      return addGizmo(new LineGizmo(p_453752_, p_451524_, p_456604_, 3.0F));
   }

   public static GizmoProperties line(Vec3 p_453265_, Vec3 p_456948_, int p_453753_, float p_451070_) {
      return addGizmo(new LineGizmo(p_453265_, p_456948_, p_453753_, p_451070_));
   }

   public static GizmoProperties arrow(Vec3 p_451107_, Vec3 p_456373_, int p_459978_) {
      return addGizmo(new ArrowGizmo(p_451107_, p_456373_, p_459978_, 2.5F));
   }

   public static GizmoProperties arrow(Vec3 p_459996_, Vec3 p_453999_, int p_459012_, float p_459851_) {
      return addGizmo(new ArrowGizmo(p_459996_, p_453999_, p_459012_, p_459851_));
   }

   public static GizmoProperties rect(Vec3 p_453807_, Vec3 p_456875_, Direction p_451147_, GizmoStyle p_452421_) {
      return addGizmo(RectGizmo.fromCuboidFace(p_453807_, p_456875_, p_451147_, p_452421_));
   }

   public static GizmoProperties rect(Vec3 p_458723_, Vec3 p_451764_, Vec3 p_455555_, Vec3 p_456457_, GizmoStyle p_459290_) {
      return addGizmo(new RectGizmo(p_458723_, p_451764_, p_455555_, p_456457_, p_459290_));
   }

   public static GizmoProperties point(Vec3 p_452529_, int p_451077_, float p_460274_) {
      return addGizmo(new PointGizmo(p_452529_, p_451077_, p_460274_));
   }

   public static GizmoProperties billboardTextOverBlock(String p_451263_, BlockPos p_452159_, int p_457853_, int p_454364_, float p_458034_) {
      double d0 = 1.3;
      double d1 = 0.2;
      GizmoProperties gizmoproperties = billboardText(
         p_451263_,
         Vec3.atLowerCornerWithOffset(p_452159_, 0.5, 1.3 + p_457853_ * 0.2, 0.5),
         TextGizmo.Style.forColorAndCentered(p_454364_).withScale(p_458034_)
      );
      gizmoproperties.setAlwaysOnTop();
      return gizmoproperties;
   }

   public static GizmoProperties billboardTextOverMob(Entity p_458337_, int p_452655_, String p_458969_, int p_458245_, float p_458947_) {
      double d0 = 2.4;
      double d1 = 0.25;
      double d2 = p_458337_.getBlockX() + 0.5;
      double d3 = p_458337_.getY() + 2.4 + p_452655_ * 0.25;
      double d4 = p_458337_.getBlockZ() + 0.5;
      float f = 0.5F;
      GizmoProperties gizmoproperties = billboardText(
         p_458969_, new Vec3(d2, d3, d4), TextGizmo.Style.forColor(p_458245_).withScale(p_458947_).withLeftAlignment(0.5F)
      );
      gizmoproperties.setAlwaysOnTop();
      return gizmoproperties;
   }

   public static GizmoProperties billboardText(String p_460075_, Vec3 p_450827_, TextGizmo.Style p_459248_) {
      return addGizmo(new TextGizmo(p_450827_, p_460075_, p_459248_));
   }

   public static class TemporaryCollection implements AutoCloseable {
      private final @Nullable GizmoCollector old = Gizmos.collector.get();
      private boolean closed;

      TemporaryCollection() {
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            Gizmos.collector.set(this.old);
         }
      }
   }
}
