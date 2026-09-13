package dev.miru.options.modes;

import dev.miru.main.ModMain;
import java.util.function.Function;
import net.minecraft.world.level.Level;

public enum LevelTypeReportMode {
   Always(
      level -> true,
      () -> ModMain.getI18N("options.report_as_debug_world.always.display_name"),
      () -> ModMain.getI18N("options.report_as_debug_world.always.hint")
   ),
   Never(
      level -> false,
      () -> ModMain.getI18N("options.report_as_debug_world.never.display_name"),
      () -> ModMain.getI18N("options.report_as_debug_world.never.hint")
   ),
   Detect(
      level -> level.isDebug,
      () -> ModMain.getI18N("options.report_as_debug_world.detect.display_name"),
      () -> ModMain.getI18N("options.report_as_debug_world.detect.hint")
   );

   public final Function<Level, Boolean> applyFunc;
   private final LevelTypeReportMode.LocalizedText displayNameSupplier;
   private final LevelTypeReportMode.LocalizedText hintSupplier;
   public static LevelTypeReportMode[] VALUES = values();

   public String getDisplayName() {
      return this.displayNameSupplier.get();
   }

   public String getHint() {
      return this.hintSupplier.get();
   }

   public LevelTypeReportMode next() {
      return VALUES[(this.ordinal() + 1) % VALUES.length];
   }

   LevelTypeReportMode(Function<Level, Boolean> func, LevelTypeReportMode.LocalizedText displayNameSupplier, LevelTypeReportMode.LocalizedText hintSupplier) {
      this.applyFunc = func;
      this.displayNameSupplier = displayNameSupplier;
      this.hintSupplier = hintSupplier;
   }

   @Override
   public String toString() {
      return this.getDisplayName();
   }

   @FunctionalInterface
   interface LocalizedText {
      String get();
   }
}
