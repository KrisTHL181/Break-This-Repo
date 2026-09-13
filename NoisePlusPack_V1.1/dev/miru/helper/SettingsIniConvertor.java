package dev.miru.helper;

import dev.miru.options.TppSettings;
import java.util.HashMap;

public class SettingsIniConvertor {
   private TppSettings tppSettings;

   public SettingsIniConvertor(TppSettings tppSettings) {
      if (tppSettings == null) {
         throw new NullPointerException("Couldn't construct from null setting object");
      }

      this.tppSettings = tppSettings;
   }

   public String toIni() {
      double[] scaler = this.tppSettings.getScalerAsArray();
      double[] offset = this.tppSettings.getOffsetAsArray();
      return "%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n%s=%s\n"
         .formatted(
            this.tppSettings.getScaler().xId(),
            scaler[0],
            this.tppSettings.getScaler().yId(),
            scaler[1],
            this.tppSettings.getScaler().zId(),
            scaler[2],
            this.tppSettings.getOffset().xId(),
            offset[0],
            this.tppSettings.getOffset().yId(),
            offset[1],
            this.tppSettings.getOffset().zId(),
            offset[2],
            this.tppSettings.getWgen_modifyMode().settingId(),
            this.tppSettings.getWgen_modifyModeValue().name(),
            this.tppSettings.getWgen_wrapMode().settingId(),
            this.tppSettings.getWgen_wrapModeValue().name(),
            this.tppSettings.getWgen_wrapDivision().settingId(),
            this.tppSettings.getWgen_wrapDivisionValue(),
            this.tppSettings.getWgen_lerpMode().settingId(),
            this.tppSettings.getWgen_lerpModeValue().name(),
            this.tppSettings.getWgen_lerpRate().settingId(),
            this.tppSettings.getWgen_lerpRateValue(),
            this.tppSettings.getWgen_amplitude().settingId(),
            this.tppSettings.getWgen_amplitudeValue(),
            this.tppSettings.getWgen_frequency().settingId(),
            this.tppSettings.getWgen_frequencyValue(),
            this.tppSettings.getWgen_minLimitNoiseCounterDivision().settingId(),
            this.tppSettings.getWgen_minLimitNoiseCounterDivisionValue(),
            this.tppSettings.getWgen_maxLimitNoiseCounterDivision().settingId(),
            this.tppSettings.getWgen_maxLimitNoiseCounterDivisionValue(),
            this.tppSettings.getExpd_demoMode().settingId(),
            this.tppSettings.getExpd_demoModeValue(),
            this.tppSettings.getExpd_debugOverlay().settingId(),
            this.tppSettings.getExpd_debugOverlayValue(),
            this.tppSettings.getExpd_reportWorldAsDebugWorld().settingId(),
            this.tppSettings.getExpd_reportWorldAsDebugWorldValue().name(),
            this.tppSettings.getExpd_enableEnhancedPauseScreen().settingId(),
            this.tppSettings.getExpd_enableEnhancedPauseScreenValue(),
            this.tppSettings.getExpd_enableToolBar().settingId(),
            this.tppSettings.getExpd_EnableToolBarValue(),
            this.tppSettings.getEnpd_enableOperateMenu().settingId(),
            this.tppSettings.getExpd_enableOperateMenuValue(),
            this.tppSettings.getExpd_noRealmsErrorScreen().settingId(),
            this.tppSettings.getExpd_noRealmsErrorScreenValue()
         );
   }

   public TppSettings fromIni(String ini) {
      if (ini == null) {
         return this.tppSettings;
      }

      String[] inis = ini.split("\n");
      HashMap<String, String> map = new HashMap<>();

      for (String string : inis) {
         if (string != null) {
            String line = string.trim();
            if (!line.isEmpty()) {
               String[] sp = line.split("=", 2);
               if (sp.length == 2) {
                  map.put(sp[0].trim(), sp[1].trim());
               }
            }
         }
      }

      String scalerXId = this.tppSettings.getScaler().xId();
      String scalerYId = this.tppSettings.getScaler().yId();
      String scalerZId = this.tppSettings.getScaler().zId();
      String offsetXId = this.tppSettings.getOffset().xId();
      String offsetYId = this.tppSettings.getOffset().yId();
      String offsetZId = this.tppSettings.getOffset().zId();

      try {
         if (map.containsKey(scalerXId)) {
            this.tppSettings.setScalerX(Double.parseDouble(map.get(scalerXId)));
         }
      } catch (Exception var32) {
      }

      try {
         if (map.containsKey(scalerYId)) {
            this.tppSettings.setScalerY(Double.parseDouble(map.get(scalerYId)));
         }
      } catch (Exception var31) {
      }

      try {
         if (map.containsKey(scalerZId)) {
            this.tppSettings.setScalerZ(Double.parseDouble(map.get(scalerZId)));
         }
      } catch (Exception var30) {
      }

      try {
         if (map.containsKey(offsetXId)) {
            this.tppSettings.setOffsetX(Double.parseDouble(map.get(offsetXId)));
         }
      } catch (Exception var29) {
      }

      try {
         if (map.containsKey(offsetYId)) {
            this.tppSettings.setOffsetY(Double.parseDouble(map.get(offsetYId)));
         }
      } catch (Exception var28) {
      }

      try {
         if (map.containsKey(offsetZId)) {
            this.tppSettings.setOffsetZ(Double.parseDouble(map.get(offsetZId)));
         }
      } catch (Exception var27) {
      }

      try {
         this.tppSettings.getWgen_modifyMode().parse(map);
      } catch (Exception var26) {
      }

      try {
         this.tppSettings.getWgen_wrapMode().parse(map);
      } catch (Exception var25) {
      }

      try {
         this.tppSettings.getWgen_wrapDivision().parse(map);
      } catch (Exception var24) {
      }

      try {
         this.tppSettings.getWgen_lerpMode().parse(map);
      } catch (Exception var23) {
      }

      try {
         this.tppSettings.getWgen_lerpRate().parse(map);
      } catch (Exception var22) {
      }

      try {
         this.tppSettings.getWgen_amplitude().parse(map);
      } catch (Exception var21) {
      }

      try {
         this.tppSettings.getWgen_frequency().parse(map);
      } catch (Exception var20) {
      }

      try {
         this.tppSettings.getWgen_minLimitNoiseCounterDivision().parse(map);
      } catch (Exception var19) {
      }

      try {
         this.tppSettings.getWgen_maxLimitNoiseCounterDivision().parse(map);
      } catch (Exception var18) {
      }

      try {
         this.tppSettings.getExpd_demoMode().parse(map);
      } catch (Exception var17) {
      }

      try {
         this.tppSettings.getExpd_debugOverlay().parse(map);
      } catch (Exception var16) {
      }

      try {
         this.tppSettings.getExpd_reportWorldAsDebugWorld().parse(map);
      } catch (Exception var15) {
      }

      try {
         this.tppSettings.getExpd_enableEnhancedPauseScreen().parse(map);
      } catch (Exception var14) {
      }

      try {
         this.tppSettings.getExpd_enableToolBar().parse(map);
      } catch (Exception var13) {
      }

      try {
         this.tppSettings.getEnpd_enableOperateMenu().parse(map);
      } catch (Exception var12) {
      }

      try {
         this.tppSettings.getExpd_noRealmsErrorScreen().parse(map);
      } catch (Exception var11) {
      }

      return this.tppSettings;
   }
}
