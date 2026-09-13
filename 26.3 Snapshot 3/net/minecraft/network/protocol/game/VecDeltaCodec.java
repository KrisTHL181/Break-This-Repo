package net.minecraft.network.protocol.game;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.PositionStep;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class VecDeltaCodec {
   private static final double TRUNCATION_STEPS = 4096.0;
   private Vec3 base = Vec3.ZERO;

   @VisibleForTesting
   static long encode(final double input) {
      return Math.round(input * 4096.0);
   }

   @VisibleForTesting
   static double decode(final long v) {
      return v / 4096.0;
   }

   public static double encodingPrecisionLoss(final double d) {
      return decode(encode(d)) - d;
   }

   public static boolean isDeltaTooBig(final long xa, final long ya, final long za) {
      return xa < -32768L || xa > 32767L || ya < -32768L || ya > 32767L || za < -32768L || za > 32767L;
   }

   public Vec3 decode(final long xa, final long ya, final long za) {
      if (xa == 0L && ya == 0L && za == 0L) {
         return this.base;
      }

      double x = xa == 0L ? this.base.x : decode(encode(this.base.x) + xa);
      double y = ya == 0L ? this.base.y : decode(encode(this.base.y) + ya);
      double z = za == 0L ? this.base.z : decode(encode(this.base.z) + za);
      return new Vec3(x, y, z);
   }

   // ===== 修改：简化 Pattern Matching 分支，移除未定义变量 var14 =====
   public @Nullable VecDelta tryEncode(final PositionPath position) {
      return switch (position) {
         case PositionPath.Linear(Vec3 pos) -> this.tryEncode(pos);
         case PositionPath.Stepped stepped -> {
            stepped.endPosition(); // 保留原副作用调用（若存在）
            List<PositionStep> steps = stepped.steps();
            yield VecDelta.Stepped.tryEncode(this, steps);
         }
         default -> throw new MatchException(null, null);
      };
   }

   public @Nullable VecDelta tryEncode(final Vec3 pos) {
      long xa = this.encodeX(pos);
      long ya = this.encodeY(pos);
      long za = this.encodeZ(pos);
      return isDeltaTooBig(xa, ya, za) ? null : new VecDelta.Linear((short)xa, (short)ya, (short)za);
   }

   public long encodeX(final Vec3 pos) {
      return encode(pos.x) - encode(this.base.x);
   }

   public long encodeY(final Vec3 pos) {
      return encode(pos.y) - encode(this.base.y);
   }

   public long encodeZ(final Vec3 pos) {
      return encode(pos.z) - encode(this.base.z);
   }

   public Vec3 delta(final Vec3 pos) {
      return pos.subtract(this.base);
   }

   public void setBase(final Vec3 base) {
      this.base = base;
   }

   public Vec3 getBase() {
      return this.base;
   }
}