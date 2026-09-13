package net.minecraft.world.phys.shapes;

import com.mojang.math.OctahedralGroup;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import org.joml.Vector3i;

public abstract class DiscreteVoxelShape {
   private static final Direction.Axis[] AXIS_VALUES = Direction.Axis.values();
   protected final int xSize;
   protected final int ySize;
   protected final int zSize;

   protected DiscreteVoxelShape(final int xSize, final int ySize, final int zSize) {
      if (xSize >= 0 && ySize >= 0 && zSize >= 0) {
         this.xSize = xSize;
         this.ySize = ySize;
         this.zSize = zSize;
      } else {
         throw new IllegalArgumentException("Need all positive sizes: x: " + xSize + ", y: " + ySize + ", z: " + zSize);
      }
   }

   public DiscreteVoxelShape rotate(final OctahedralGroup rotation) {
      if (rotation == OctahedralGroup.IDENTITY) {
         return this;
      }

      Vector3i v = rotation.rotate(new Vector3i(this.xSize, this.ySize, this.zSize));
      int shiftX = fixupCoordinate(v, 0);
      int shiftY = fixupCoordinate(v, 1);
      int shiftZ = fixupCoordinate(v, 2);
      DiscreteVoxelShape newShape = new BitSetDiscreteVoxelShape(v.x, v.y, v.z);

      for (int x = 0; x < this.xSize; x++) {
         for (int y = 0; y < this.ySize; y++) {
            for (int z = 0; z < this.zSize; z++) {
               if (this.isFull(x, y, z)) {
                  Vector3i newPos = rotation.rotate(v.set(x, y, z));
                  int newX = shiftX + newPos.x;
                  int newY = shiftY + newPos.y;
                  int newZ = shiftZ + newPos.z;
                  newShape.fill(newX, newY, newZ);
               }
            }
         }
      }

      return newShape;
   }

   private static int fixupCoordinate(final Vector3i v, final int index) {
      int value = v.get(index);
      if (value < 0) {
         v.setComponent(index, -value);
         return -value - 1;
      } else {
         return 0;
      }
   }

   public boolean isFullWide(final AxisCycle transform, final int x, final int y, final int z) {
      return this.isFullWide(transform.cycle(x, y, z, Direction.Axis.X), transform.cycle(x, y, z, Direction.Axis.Y), transform.cycle(x, y, z, Direction.Axis.Z));
   }

   public boolean isFullWide(final int x, final int y, final int z) {
      if (x < 0 || y < 0 || z < 0) {
         return false;
      } else {
         return x < this.xSize && y < this.ySize && z < this.zSize ? this.isFull(x, y, z) : false;
      }
   }

   public boolean isFull(final AxisCycle transform, final int x, final int y, final int z) {
      return this.isFull(transform.cycle(x, y, z, Direction.Axis.X), transform.cycle(x, y, z, Direction.Axis.Y), transform.cycle(x, y, z, Direction.Axis.Z));
   }

   public abstract boolean isFull(final int x, final int y, final int z);

   public abstract void fill(final int x, final int y, final int z);

   public boolean isEmpty() {
      for (Direction.Axis axis : AXIS_VALUES) {
         if (this.firstFull(axis) >= this.lastFull(axis)) {
            return true;
         }
      }

      return false;
   }

   public abstract int firstFull(final Direction.Axis axis);

   public abstract int lastFull(final Direction.Axis axis);

   public int firstFull(final Direction.Axis aAxis, final int b, final int c) {
      int aSize = this.getSize(aAxis);
      if (b >= 0 && c >= 0) {
         Direction.Axis bAxis = AxisCycle.FORWARD.cycle(aAxis);
         Direction.Axis cAxis = AxisCycle.BACKWARD.cycle(aAxis);
         if (b < this.getSize(bAxis) && c < this.getSize(cAxis)) {
            AxisCycle transform = AxisCycle.between(Direction.Axis.X, aAxis);

            for (int a = 0; a < aSize; a++) {
               if (this.isFull(transform, a, b, c)) {
                  return a;
               }
            }

            return aSize;
         } else {
            return aSize;
         }
      } else {
         return aSize;
      }
   }

   public int lastFull(final Direction.Axis aAxis, final int b, final int c) {
      if (b >= 0 && c >= 0) {
         Direction.Axis bAxis = AxisCycle.FORWARD.cycle(aAxis);
         Direction.Axis cAxis = AxisCycle.BACKWARD.cycle(aAxis);
         if (b < this.getSize(bAxis) && c < this.getSize(cAxis)) {
            int aSize = this.getSize(aAxis);
            AxisCycle transform = AxisCycle.between(Direction.Axis.X, aAxis);

            for (int a = aSize - 1; a >= 0; a--) {
               if (this.isFull(transform, a, b, c)) {
                  return a + 1;
               }
            }

            return 0;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public int getSize(final Direction.Axis axis) {
      return axis.choose(this.xSize, this.ySize, this.zSize);
   }

   public int getXSize() {
      return this.getSize(Direction.Axis.X);
   }

   public int getYSize() {
      return this.getSize(Direction.Axis.Y);
   }

   public int getZSize() {
      return this.getSize(Direction.Axis.Z);
   }

   public void forAllEdges(final DiscreteVoxelShape.IntLineConsumer consumer, final boolean mergeNeighbors) {
      this.forAllAxisEdges(consumer, AxisCycle.NONE, mergeNeighbors);
      this.forAllAxisEdges(consumer, AxisCycle.FORWARD, mergeNeighbors);
      this.forAllAxisEdges(consumer, AxisCycle.BACKWARD, mergeNeighbors);
   }

   private void forAllAxisEdges(final DiscreteVoxelShape.IntLineConsumer consumer, final AxisCycle transform, final boolean mergeNeighbors) {
      AxisCycle inverse = transform.inverse();
      int aSize = this.getSize(inverse.cycle(Direction.Axis.X));
      int bSize = this.getSize(inverse.cycle(Direction.Axis.Y));
      int cSize = this.getSize(inverse.cycle(Direction.Axis.Z));

      for (int a = 0; a <= aSize; a++) {
         for (int b = 0; b <= bSize; b++) {
            int lastStart = -1;

            for (int c = 0; c <= cSize; c++) {
               int fullSectors = 0;
               int oddSectors = 0;

               for (int da = 0; da <= 1; da++) {
                  for (int db = 0; db <= 1; db++) {
                     if (this.isFullWide(inverse, a + da - 1, b + db - 1, c)) {
                        fullSectors++;
                        oddSectors ^= da ^ db;
                     }
                  }
               }

               if (fullSectors == 1 || fullSectors == 3 || fullSectors == 2 && (oddSectors & 1) == 0) {
                  if (mergeNeighbors) {
                     if (lastStart == -1) {
                        lastStart = c;
                     }
                  } else {
                     consumer.consume(
                        inverse.cycle(a, b, c, Direction.Axis.X),
                        inverse.cycle(a, b, c, Direction.Axis.Y),
                        inverse.cycle(a, b, c, Direction.Axis.Z),
                        inverse.cycle(a, b, c + 1, Direction.Axis.X),
                        inverse.cycle(a, b, c + 1, Direction.Axis.Y),
                        inverse.cycle(a, b, c + 1, Direction.Axis.Z)
                     );
                  }
               } else if (lastStart != -1) {
                  consumer.consume(
                     inverse.cycle(a, b, lastStart, Direction.Axis.X),
                     inverse.cycle(a, b, lastStart, Direction.Axis.Y),
                     inverse.cycle(a, b, lastStart, Direction.Axis.Z),
                     inverse.cycle(a, b, c, Direction.Axis.X),
                     inverse.cycle(a, b, c, Direction.Axis.Y),
                     inverse.cycle(a, b, c, Direction.Axis.Z)
                  );
                  lastStart = -1;
               }
            }
         }
      }
   }

   public void forAllBoxes(final DiscreteVoxelShape.IntLineConsumer consumer, final boolean mergeNeighbors) {
      BitSetDiscreteVoxelShape.forAllBoxes(this, consumer, mergeNeighbors);
   }

   public void forAllFaces(final DiscreteVoxelShape.IntFaceConsumer consumer) {
      this.forAllAxisFaces(consumer, AxisCycle.NONE);
      this.forAllAxisFaces(consumer, AxisCycle.FORWARD);
      this.forAllAxisFaces(consumer, AxisCycle.BACKWARD);
   }

   private void forAllAxisFaces(final DiscreteVoxelShape.IntFaceConsumer consumer, final AxisCycle transform) {
      AxisCycle inverse = transform.inverse();
      Direction.Axis cAxis = inverse.cycle(Direction.Axis.Z);
      int aSize = this.getSize(inverse.cycle(Direction.Axis.X));
      int bSize = this.getSize(inverse.cycle(Direction.Axis.Y));
      int cSize = this.getSize(cAxis);
      Direction negative = Direction.fromAxisAndDirection(cAxis, Direction.AxisDirection.NEGATIVE);
      Direction positive = Direction.fromAxisAndDirection(cAxis, Direction.AxisDirection.POSITIVE);

      for (int a = 0; a < aSize; a++) {
         for (int b = 0; b < bSize; b++) {
            boolean lastFull = false;

            for (int c = 0; c <= cSize; c++) {
               boolean full = c != cSize && this.isFull(inverse, a, b, c);
               if (!lastFull && full) {
                  consumer.consume(
                     negative, inverse.cycle(a, b, c, Direction.Axis.X), inverse.cycle(a, b, c, Direction.Axis.Y), inverse.cycle(a, b, c, Direction.Axis.Z)
                  );
               }

               if (lastFull && !full) {
                  consumer.consume(
                     positive,
                     inverse.cycle(a, b, c - 1, Direction.Axis.X),
                     inverse.cycle(a, b, c - 1, Direction.Axis.Y),
                     inverse.cycle(a, b, c - 1, Direction.Axis.Z)
                  );
               }

               lastFull = full;
            }
         }
      }
   }

   public interface IntFaceConsumer {
      void consume(Direction direction, int x, int y, int z);
   }

   public interface IntLineConsumer {
      void consume(int x1, int y1, int z1, int x2, int y2, int z2);
   }
}
