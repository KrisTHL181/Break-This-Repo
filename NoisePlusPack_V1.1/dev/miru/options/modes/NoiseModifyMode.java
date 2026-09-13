package dev.miru.options.modes;

import dev.miru.helper.TriFunction;
import dev.miru.main.ModMain;

public enum NoiseModifyMode {
   ScaleOffset(
      () -> ModMain.getI18N("options.noise.modify.mode.scale_offset.display_name"),
      () -> ModMain.getI18N("options.noise.modify.mode.scale_offset.hint"),
      true,
      true,
      (orig, scale, offset) -> orig * scale + offset,
      (proc, scale, offset) -> (proc - offset) / scale
   ),
   OffsetScale(
      () -> ModMain.getI18N("options.noise.modify.mode.offset_scale.display_name"),
      () -> ModMain.getI18N("options.noise.modify.mode.offset_scale.hint"),
      true,
      true,
      (orig, scale, offset) -> (orig + offset) * scale,
      (proc, scale, offset) -> proc / scale - offset
   ),
   ScaleOnly(
      () -> ModMain.getI18N("options.noise.modify.mode.scale_only.display_name"),
      () -> ModMain.getI18N("options.noise.modify.mode.scale_only.hint"),
      true,
      false,
      (orig, scale, offset) -> orig * scale,
      (proc, scale, offset) -> proc / scale
   ),
   OffsetOnly(
      () -> ModMain.getI18N("options.noise.modify.mode.offset_only.display_name"),
      () -> ModMain.getI18N("options.noise.modify.mode.offset_only.hint"),
      false,
      true,
      (orig, scale, offset) -> orig + offset,
      (proc, scale, offset) -> proc - offset
   );

   public final TriFunction<Double, Double, Double, Double> applyFunction;
   public final TriFunction<Double, Double, Double, Double> reverseFunction;
   private final NoiseModifyMode.LocalizedText displayNameSupplier;
   private final NoiseModifyMode.LocalizedText hintSupplier;
   public final boolean allowInputScale;
   public final boolean allowInputOffset;
   public static final NoiseModifyMode[] VALUES = values();

   NoiseModifyMode(
      NoiseModifyMode.LocalizedText displayNameSupplier,
      NoiseModifyMode.LocalizedText hintSupplier,
      boolean allowInputScale,
      boolean allowInputOffset,
      TriFunction<Double, Double, Double, Double> func,
      TriFunction<Double, Double, Double, Double> reverseFunc
   ) {
      this.applyFunction = func;
      this.reverseFunction = reverseFunc;
      this.displayNameSupplier = displayNameSupplier;
      this.hintSupplier = hintSupplier;
      this.allowInputOffset = allowInputOffset;
      this.allowInputScale = allowInputScale;
   }

   public String getDescribe() {
      return this.displayNameSupplier.get();
   }

   public String getHint() {
      return this.hintSupplier.get();
   }

   public NoiseModifyMode next() {
      return VALUES[(this.ordinal() + 1) % VALUES.length];
   }

   public double reverseApply(double processedValue, double scale, double offset) {
      if (scale == 0.0) {
         return Double.POSITIVE_INFINITY;
      }

      if (Double.isNaN(processedValue) || Double.isNaN(scale) || Double.isNaN(offset)) {
         return Double.NaN;
      }

      if (offset != Double.POSITIVE_INFINITY && offset != Double.NEGATIVE_INFINITY && scale != Double.POSITIVE_INFINITY && scale != Double.NEGATIVE_INFINITY) {
         try {
            return this.reverseFunction.apply(processedValue, scale, offset);
         } catch (ArithmeticException e) {
            return Double.NaN;
         }
      } else {
         return Double.NaN;
      }
   }

   @Override
   public String toString() {
      return this.getDescribe();
   }

   @FunctionalInterface
   interface LocalizedText {
      String get();
   }
}
