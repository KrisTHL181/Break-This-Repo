package net.minecraft.core;

public class Cursor3D {
   public static final int TYPE_INSIDE = 0;
   public static final int TYPE_FACE = 1;
   public static final int TYPE_EDGE = 2;
   public static final int TYPE_CORNER = 3;
   private final int originX;
   private final int originY;
   private final int originZ;
   private final int width;
   private final int height;
   private final int depth;
   private final int end;
   private int index;
   private int x;
   private int y;
   private int z;

   public Cursor3D(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
      this.originX = minX;
      this.originY = minY;
      this.originZ = minZ;
      this.width = maxX - minX + 1;
      this.height = maxY - minY + 1;
      this.depth = maxZ - minZ + 1;
      this.end = this.width * this.height * this.depth;
   }

   public boolean advance() {
      if (this.index == this.end) {
         return false;
      }

      this.x = this.index % this.width;
      int slice = this.index / this.width;
      this.y = slice % this.height;
      this.z = slice / this.height;
      this.index++;
      return true;
   }

   public int nextX() {
      return this.originX + this.x;
   }

   public int nextY() {
      return this.originY + this.y;
   }

   public int nextZ() {
      return this.originZ + this.z;
   }

   public int getNextType() {
      int type = 0;
      if (this.x == 0 || this.x == this.width - 1) {
         type++;
      }

      if (this.y == 0 || this.y == this.height - 1) {
         type++;
      }

      if (this.z == 0 || this.z == this.depth - 1) {
         type++;
      }

      return type;
   }
}
