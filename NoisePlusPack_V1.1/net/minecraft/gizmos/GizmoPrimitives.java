package net.minecraft.gizmos;

import net.minecraft.world.phys.Vec3;

public interface GizmoPrimitives {
   void addPoint(Vec3 var1, int var2, float var3);

   void addLine(Vec3 var1, Vec3 var2, int var3, float var4);

   void addTriangleFan(Vec3[] var1, int var2);

   void addQuad(Vec3 var1, Vec3 var2, Vec3 var3, Vec3 var4, int var5);

   void addText(Vec3 var1, String var2, TextGizmo.Style var3);
}
