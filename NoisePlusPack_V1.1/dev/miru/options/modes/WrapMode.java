package dev.miru.options.modes;

import dev.miru.main.ModMain;
import java.util.function.BiFunction;
import net.minecraft.util.Mth;

public enum WrapMode {
   Vanilla(
      () -> ModMain.getI18N("options.wrap.mode.vanilla.display_name"),
      () -> ModMain.getI18N("options.wrap.mode.vanilla.hint"),
      false,
      (p, d) -> p - Mth.lfloor(p / 3.3554432E7 + 0.5) * 3.3554432E7
   ),
   CusDiv(
      () -> ModMain.getI18N("options.wrap.mode.cus_div.display_name"),
      () -> ModMain.getI18N("options.wrap.mode.cus_div.hint"),
      true,
      (p, d) -> p - Mth.floor(p / d + 0.5) * d
   ),
   RawPos(() -> ModMain.getI18N("options.wrap.mode.raw_pos.display_name"), () -> ModMain.getI18N("options.wrap.mode.raw_pos.hint"), false, (p, d) -> p);

   public final BiFunction<Double, Double, Double> func;
   private final WrapMode.LocalizedText nameSupplier;
   private final WrapMode.LocalizedText hintSupplier;
   public final boolean allowInputDiv;
   public static final WrapMode[] VALUES = values();

   WrapMode(WrapMode.LocalizedText nameSupplier, WrapMode.LocalizedText hintSupplier, boolean allowInputDiv, BiFunction<Double, Double, Double> func) {
      this.func = func;
      this.nameSupplier = nameSupplier;
      this.hintSupplier = hintSupplier;
      this.allowInputDiv = allowInputDiv;
   }

   public String getName() {
      return this.nameSupplier.get();
   }

   public String getHint() {
      return this.hintSupplier.get();
   }

   public WrapMode next() {
      return VALUES[(this.ordinal() + 1) % VALUES.length];
   }

   @Override
   public String toString() {
      return this.getName();
   }

   @FunctionalInterface
   interface LocalizedText {
      String get();
   }
}
