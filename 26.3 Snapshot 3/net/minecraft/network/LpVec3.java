package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LpVec3 {
   private static final int DATA_BITS = 15;
   private static final int DATA_BITS_MASK = 32767;
   private static final double MAX_QUANTIZED_VALUE = 32766.0;
   private static final int SCALE_BITS = 2;
   private static final int SCALE_BITS_MASK = 3;
   private static final int CONTINUATION_FLAG = 4;
   private static final int X_OFFSET = 3;
   private static final int Y_OFFSET = 18;
   private static final int Z_OFFSET = 33;
   public static final double ABS_MAX_VALUE = 1.7179869183E10;
   public static final double ABS_MIN_VALUE = 3.051944088384301E-5;

   public static boolean hasContinuationBit(final int in) {
      return (in & 4) == 4;
   }

   public static Vec3 read(final ByteBuf input) {
      int lowest = input.readUnsignedByte();
      if (lowest == 0) {
         return Vec3.ZERO;
      }

      int middle = input.readUnsignedByte();
      long highest = input.readUnsignedInt();
      long buffer = highest << 16 | middle << 8 | lowest;
      long scale = lowest & 3;
      if (hasContinuationBit(lowest)) {
         scale |= (VarInt.read(input) & 4294967295L) << 2;
      }

      return new Vec3(unpack(buffer >> 3) * scale, unpack(buffer >> 18) * scale, unpack(buffer >> 33) * scale);
   }

   public static void write(final ByteBuf output, final Vec3 value) {
      double x = sanitize(value.x);
      double y = sanitize(value.y);
      double z = sanitize(value.z);
      double chessboardLength = Mth.absMax(x, Mth.absMax(y, z));
      if (chessboardLength < 3.051944088384301E-5) {
         output.writeByte(0);
      } else {
         long scale = Mth.ceilLong(chessboardLength);
         boolean isPartial = (scale & 3L) != scale;
         long markers = isPartial ? scale & 3L | 4L : scale;
         long xn = pack(x / scale) << 3;
         long yn = pack(y / scale) << 18;
         long zn = pack(z / scale) << 33;
         long buffer = markers | xn | yn | zn;
         output.writeByte((byte)buffer);
         output.writeByte((byte)(buffer >> 8));
         output.writeInt((int)(buffer >> 16));
         if (isPartial) {
            VarInt.write(output, (int)(scale >> 2));
         }
      }
   }

   private static double sanitize(final double value) {
      return Double.isNaN(value) ? 0.0 : Math.clamp(value, -1.7179869183E10, 1.7179869183E10);
   }

   private static long pack(final double value) {
      return Math.round((value * 0.5 + 0.5) * 32766.0);
   }

   private static double unpack(final long value) {
      return Math.min(value & 32767L, 32766.0) * 2.0 / 32766.0 - 1.0;
   }
}
