package dev.miru.main;

import dev.miru.localization.TppTranslateManager;
import dev.miru.options.TppSettings;
import net.minecraft.network.chat.Component;

public class ModMain {
   private static TppTranslateManager translateManager = new TppTranslateManager();
   private static TppSettings options = new TppSettings();

   public static TppSettings getOptions() {
      return options;
   }

   public static void writeSettings(TppSettings optn) {
      if (optn != null) {
         options.setScalerX(optn.getScalerX());
         options.setScalerY(optn.getScalerY());
         options.setScalerZ(optn.getScalerZ());
         options.setOffsetX(optn.getOffsetX());
         options.setOffsetY(optn.getOffsetY());
         options.setOffsetZ(optn.getOffsetZ());
         options.setWgen_amplitude(optn.getWgen_amplitudeValue());
         options.setWgen_frequency(optn.getWgen_frequencyValue());
         options.setWgen_lerpRate(optn.getWgen_lerpRateValue());
         options.setWgen_wrapDivision(optn.getWgen_wrapDivisionValue());
         options.setWgen_minLimitNoiseCounterDivision(optn.getWgen_minLimitNoiseCounterDivisionValue());
         options.setWgen_maxLimitNoiseCounterDivision(optn.getWgen_maxLimitNoiseCounterDivisionValue());
         options.setExpd_demoMode(optn.getExpd_demoModeValue());
         options.setExpd_debugOverlay(optn.getExpd_debugOverlayValue());
         options.setExpd_reportWorldAsDebugWorld(optn.getExpd_reportWorldAsDebugWorldValue());
         options.setExpd_enableEnhancedPauseScreen(optn.getExpd_enableEnhancedPauseScreenValue());
         options.setExpd_enableToolBar(optn.getExpd_EnableToolBarValue());
         options.setExpd_enableOperateMenu(optn.getExpd_enableOperateMenuValue());
         options.setExpd_noRealmsErrorScreen(optn.getExpd_noRealmsErrorScreenValue());
         options.setWgen_wrapMode(optn.getWgen_wrapModeValue());
         options.setWgen_modifyMode(optn.getWgen_modifyModeValue());
         options.setWgen_lerpMode(optn.getWgen_lerpModeValue());
      }
   }

   public static void resetSettings() {
      options = new TppSettings();
   }

   public static TppTranslateManager getTranslateManager() {
      return translateManager;
   }

   public static String getI18N(String key, Object... args) {
      return getTranslateManager().getI18N(key, args);
   }

   public static String getI18N(String key) {
      return getI18N(key);
   }

   public static Component getI18nAsComponent(String key, Object... args) {
      return Component.literal(getI18N(key, args));
   }

   public static Component getI18nAsComponent(String key) {
      return getI18nAsComponent(key);
   }
}
