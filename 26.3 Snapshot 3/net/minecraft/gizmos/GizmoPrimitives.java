package net.minecraft.gizmos;

import net.minecraft.world.phys.Vec3;

public interface GizmoPrimitives {
   void addPoint(Vec3 pos, int color, float size);

   void addLine(Vec3 start, Vec3 end, int color, float width);

   void addTriangleFan(Vec3[] points, int color);

   void addQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color);

   void addText(Vec3 pos, String text, TextGizmo.Style style);
}
