package dev.miru.options.modes;

import dev.miru.helper.TriFunction;
import dev.miru.main.ModMain;
import net.minecraft.util.Mth;

public enum LerpMode {
   StartDefaultDiv(
      () -> ModMain.getI18N("options.lerp.mode.start_div.display_name"),
      () -> ModMain.getI18N("options.lerp.mode.start_div.hint"),
      false,
      (s, e, p) -> s / 128.0
   ),
   StartCustomDiv(
      () -> ModMain.getI18N("options.lerp.mode.start_cdiv.display_name"),
      () -> ModMain.getI18N("options.lerp.mode.start_cdiv.hint"),
      true,
      (s, e, p) -> s / ModMain.getOptions().getWgen_lerpRateValue()
   ),
   StartNoDiv(
      () -> ModMain.getI18N("options.lerp.mode.start_ndiv.display_name"), () -> ModMain.getI18N("options.lerp.mode.start_ndiv.hint"), false, (s, e, p) -> s
   ),
   EndDefaultDiv(
      () -> ModMain.getI18N("options.lerp.mode.end_div.display_name"), () -> ModMain.getI18N("options.lerp.mode.end_div.hint"), false, (s, e, p) -> e / 128.0
   ),
   EndCustomDiv(
      () -> ModMain.getI18N("options.lerp.mode.end_cdiv.display_name"),
      () -> ModMain.getI18N("options.lerp.mode.end_cdiv.hint"),
      true,
      (s, e, p) -> e / ModMain.getOptions().getWgen_lerpRateValue()
   ),
   EndNoDiv(() -> ModMain.getI18N("options.lerp.mode.end_ndiv.display_name"), () -> ModMain.getI18N("options.lerp.mode.end_ndiv.hint"), false, (s, e, p) -> e),
   NormalDefaultDiv(
      () -> ModMain.getI18N("options.lerp.mode.norm_div.display_name"),
      () -> ModMain.getI18N("options.lerp.mode.norm_div.hint"),
      false,
      (s, e, p) -> Mth.clampedLerp(p, s, e) / 128.0
   ),
   NormalCustomDiv(
      () -> ModMain.getI18N("options.lerp.mode.norm_cdiv.display_name"),
      () -> ModMain.getI18N("options.lerp.mode.norm_cdiv.hint"),
      true,
      (s, e, p) -> Mth.clampedLerp(p, s, e) / ModMain.getOptions().getWgen_lerpRateValue()
   ),
   NormalNoDiv(
      () -> ModMain.getI18N("options.lerp.mode.norm_ndiv.display_name"),
      () -> ModMain.getI18N("options.lerp.mode.norm_ndiv.hint"),
      false,
      (s, e, p) -> Mth.clampedLerp(p, s, e)
   ),
   ProgressDefaultDiv(
      () -> ModMain.getI18N("options.lerp.mode.prog_div.display_name"), () -> ModMain.getI18N("options.lerp.mode.prog_div.hint"), false, (s, e, p) -> p / 128.0
   ),
   ProgressCustomDiv(
      () -> ModMain.getI18N("options.lerp.mode.prog_cdiv.display_name"),
      () -> ModMain.getI18N("options.lerp.mode.prog_cdiv.hint"),
      true,
      (s, e, p) -> p / ModMain.getOptions().getWgen_lerpRateValue()
   ),
   ProgressNoDiv(
      () -> ModMain.getI18N("options.lerp.mode.prog_ndiv.display_name"), () -> ModMain.getI18N("options.lerp.mode.prog_ndiv.hint"), false, (s, e, p) -> p
   );

   private final LerpMode.LocalizedText describeSupplier;
   private final LerpMode.LocalizedText hintSupplier;
   public final TriFunction<Double, Double, Double, Double> func;
   public final boolean allowInputRate;
   public static final LerpMode[] VALUES = values();

   LerpMode(
      LerpMode.LocalizedText describeSupplier, LerpMode.LocalizedText hintSupplier, boolean allowInputRate, TriFunction<Double, Double, Double, Double> func
   ) {
      this.describeSupplier = describeSupplier;
      this.hintSupplier = hintSupplier;
      this.func = func;
      this.allowInputRate = allowInputRate;
   }

   public String getDescribe() {
      return this.describeSupplier.get();
   }

   public String getHint() {
      return this.hintSupplier.get();
   }

   public LerpMode next() {
      return VALUES[(this.ordinal() + 1) % VALUES.length];
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
