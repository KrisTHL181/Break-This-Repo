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

   public static boolean hasContinuationBit(int p_423725_) {
      return (p_423725_ & 4) == 4;
   }

   public static Vec3 read(ByteBuf p_427966_) {
      int i = p_427966_.readUnsignedByte();
      if (i == 0) {
         return Vec3.ZERO;
      }

      int j = p_427966_.readUnsignedByte();
      long k = p_427966_.readUnsignedInt();
      long l = k << 16 | j << 8 | i;
      long i1 = i & 3;
      if (hasContinuationBit(i)) {
         i1 |= (VarInt.read(p_427966_) & 4294967295L) << 2;
      }

      return new Vec3(unpack(l >> 3) * i1, unpack(l >> 18) * i1, unpack(l >> 33) * i1);
   }

   public static void write(ByteBuf p_429046_, Vec3 p_423898_) {
      double d0 = sanitize(p_423898_.x);
      double d1 = sanitize(p_423898_.y);
      double d2 = sanitize(p_423898_.z);
      double d3 = Mth.absMax(d0, Mth.absMax(d1, d2));
      if (d3 < 3.051944088384301E-5) {
         p_429046_.writeByte(0);
      } else {
         long i = Mth.ceilLong(d3);
         boolean flag = (i & 3L) != i;
         long j = flag ? i & 3L | 4L : i;
         long k = pack(d0 / i) << 3;
         long l = pack(d1 / i) << 18;
         long i1 = pack(d2 / i) << 33;
         long j1 = j | k | l | i1;
         p_429046_.writeByte((byte)j1);
         p_429046_.writeByte((byte)(j1 >> 8));
         p_429046_.writeInt((int)(j1 >> 16));
         if (flag) {
            VarInt.write(p_429046_, (int)(i >> 2));
         }
      }
   }

   private static double sanitize(double p_424101_) {
      return Double.isNaN(p_424101_) ? 0.0 : Math.clamp(p_424101_, -1.7179869183E10, 1.7179869183E10);
   }

   private static long pack(double p_428292_) {
      return Math.round((p_428292_ * 0.5 + 0.5) * 32766.0);
   }

   private static double unpack(long p_429828_) {
      return Math.min(p_429828_ & 32767L, 32766.0) * 2.0 / 32766.0 - 1.0;
   }
}
