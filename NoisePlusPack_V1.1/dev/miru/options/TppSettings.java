package dev.miru.options;

import dev.miru.helper.SettingsIniConvertor;
import dev.miru.main.ModMain;
import dev.miru.options.base.SettingItem;
import dev.miru.options.base.Vec3Options;
import dev.miru.options.modes.LerpMode;
import dev.miru.options.modes.LevelTypeReportMode;
import dev.miru.options.modes.NoiseModifyMode;
import dev.miru.options.modes.WrapMode;
import java.util.List;

public class TppSettings {
   private Vec3Options<Double> wgen_scale = new Vec3Options<>(1.0, 1.0, 1.0)
      .setDefaultValue(1.0, 1.0, 1.0)
      .setSettingIds("wgen.scaler.x", "wgen.scaler.y", "wgen.scaler.z");
   private Vec3Options<Double> wgen_offset = new Vec3Options<>(0.0, 0.0, 0.0)
      .setDefaultValue(0.0, 0.0, 0.0)
      .setSettingIds("wgen.offset.x", "wgen.offset.y", "wgen.offset.z");
   private SettingItem<Boolean> expd_demoMode = new SettingItem<>(
         false, v -> ModMain.getI18N("options.demo.mode.desc", v ? ModMain.getI18N("common.switch.on") : ModMain.getI18N("common.switch.off"))
      )
      .setDefaultValue(false)
      .setToggleFunction(SettingItem.BOOL_REVERSE_ACTION)
      .setParseFunction(SettingItem.BOOLEAN_PARSE_ACTION)
      .settingId("expd.demo");
   private SettingItem<WrapMode> wgen_wrapMode = new SettingItem<>(WrapMode.RawPos, v -> ModMain.getI18N("options.wrap.mode.title") + " : " + v.getName())
      .setDefaultValue(WrapMode.RawPos)
      .setToggleFunction((v, s) -> s.setValue(v.next()))
      .setParseFunction(WrapMode::valueOf)
      .settingId("wgen.wrap.mode");
   private SettingItem<NoiseModifyMode> wgen_modifyMode = new SettingItem<>(
         NoiseModifyMode.ScaleOffset, v -> ModMain.getI18N("options.noise.modify.mode.desc", v.getDescribe())
      )
      .setDefaultValue(NoiseModifyMode.ScaleOffset)
      .setToggleFunction((v, s) -> s.setValue(v.next()))
      .setParseFunction(NoiseModifyMode::valueOf)
      .settingId("wgen.modify.mode");
   private SettingItem<LerpMode> wgen_lerpMode = new SettingItem<>(LerpMode.NormalDefaultDiv, v -> ModMain.getI18N("options.lerp.mode.desc", v.getDescribe()))
      .setDefaultValue(LerpMode.NormalDefaultDiv)
      .setToggleFunction((v, s) -> s.setValue(v.next()))
      .setParseFunction(LerpMode::valueOf)
      .settingId("wgen.lerp.mode");
   private SettingItem<Boolean> expd_debugOverlay = new SettingItem<>(
         false, v -> ModMain.getI18N("options.debug.overlay.desc", v ? ModMain.getI18N("common.switch.on") : ModMain.getI18N("common.switch.off"))
      )
      .setDefaultValue(false)
      .setToggleFunction(SettingItem.BOOL_REVERSE_ACTION)
      .setParseFunction(SettingItem.BOOLEAN_PARSE_ACTION)
      .settingId("expd.debug.overlay");
   private SettingItem<LevelTypeReportMode> expd_reportWorldAsDebugWorld = new SettingItem<>(
         LevelTypeReportMode.Never, v -> ModMain.getI18N("options.report_as_debug_world.desc", v.getDisplayName())
      )
      .setDefaultValue(LevelTypeReportMode.Never)
      .setToggleFunction((v, s) -> s.setValue(v.next()))
      .setParseFunction(LevelTypeReportMode::valueOf)
      .settingId("expd.debug.level.type.report.mode");
   private SettingItem<Double> wgen_amplitude = new SettingItem<>(2.0, v -> ModMain.getI18N("options.amplitude.title") + " : " + v)
      .setDefaultValue(2.0)
      .setNonToggleable()
      .setParseFunction(SettingItem.DOUBLE_PARSE_ACTION)
      .settingId("wgen.amplitude");
   private SettingItem<Double> wgen_frequency = new SettingItem<>(2.0, v -> ModMain.getI18N("options.frequency.title") + " : " + v)
      .setDefaultValue(2.0)
      .setNonToggleable()
      .setParseFunction(SettingItem.DOUBLE_PARSE_ACTION)
      .settingId("wgen.frequency");
   private SettingItem<Double> wgen_lerpRate = new SettingItem<>(128.0, v -> ModMain.getI18N("options.lerp.rate.title") + " : " + v)
      .setDefaultValue(128.0)
      .setNonToggleable()
      .setParseFunction(SettingItem.DOUBLE_PARSE_ACTION)
      .settingId("wgen.lerp.rate");
   private SettingItem<Double> wgen_minLimitNoiseCounterDivision = new SettingItem<>(512.0, v -> ModMain.getI18N("options.min_noise_counter_division.desc", v))
      .setDefaultValue(512.0)
      .setNonToggleable()
      .setParseFunction(SettingItem.DOUBLE_PARSE_ACTION)
      .settingId("wgen.noise.limit.min.div");
   private SettingItem<Double> wgen_maxLimitNoiseCounterDivision = new SettingItem<>(512.0, v -> ModMain.getI18N("options.max_noise_counter_division.desc", v))
      .setDefaultValue(512.0)
      .setNonToggleable()
      .setParseFunction(SettingItem.DOUBLE_PARSE_ACTION)
      .settingId("wgen.noise.limit.max.div");
   private SettingItem<Double> wgen_wrapDivision = new SettingItem<>(3.3554432E7, v -> ModMain.getI18N("options.wrap_division.desc", v))
      .setDefaultValue(3.3554432E7)
      .setNonToggleable()
      .setParseFunction(SettingItem.DOUBLE_PARSE_ACTION)
      .settingId("wgen.wrap.div");
   private SettingItem<Boolean> expd_enableEnhancedPauseScreen = new SettingItem<>(
         true,
         v -> ModMain.getI18N("options.enable_enhanced_pause_screen.desc", v ? ModMain.getI18N("common.switch.on") : ModMain.getI18N("common.switch.off"))
      )
      .setDefaultValue(true)
      .setToggleFunction(SettingItem.BOOL_REVERSE_ACTION)
      .setParseFunction(SettingItem.BOOLEAN_PARSE_ACTION)
      .settingId("expd.ui.pause.enchanted");
   private SettingItem<Boolean> expd_enableToolBar = new SettingItem<>(
         false, v -> ModMain.getI18N("options.enable_toolbar.desc", v ? ModMain.getI18N("common.switch.on") : ModMain.getI18N("common.switch.off"))
      )
      .setDefaultValue(false)
      .setToggleFunction(SettingItem.BOOL_REVERSE_ACTION)
      .setParseFunction(SettingItem.BOOLEAN_PARSE_ACTION)
      .settingId("expd.ui.menu.toolbar");
   private SettingItem<Boolean> enpd_enableOperateMenu = new SettingItem<>(
         false, v -> ModMain.getI18N("options.enable_operate_menu.desc", v ? ModMain.getI18N("common.switch.on") : ModMain.getI18N("common.switch.off"))
      )
      .setDefaultValue(false)
      .setToggleFunction(SettingItem.BOOL_REVERSE_ACTION)
      .setParseFunction(SettingItem.BOOLEAN_PARSE_ACTION)
      .settingId("expd.ui.menu.operate.menu");
   private SettingItem<Boolean> expd_noRealmsErrorScreen = new SettingItem<>(
         true, v -> ModMain.getI18N("options.no_realms_error_screen.desc", v ? ModMain.getI18N("common.switch.on") : ModMain.getI18N("common.switch.off"))
      )
      .setDefaultValue(true)
      .setToggleFunction(SettingItem.BOOL_REVERSE_ACTION)
      .setParseFunction(SettingItem.BOOLEAN_PARSE_ACTION)
      .settingId("expd.ui.realms.error.no");
   private SettingsIniConvertor iniConvertor = new SettingsIniConvertor(this);

   @Override
   public String toString() {
      return this.iniConvertor.toIni();
   }

   public static TppSettings parseOptions(String str) {
      try {
         SettingsIniConvertor iniConvertor1 = new SettingsIniConvertor(ModMain.getOptions());
         TppSettings loaded = iniConvertor1.fromIni(str);
         TppSettings base = new TppSettings();
         if (loaded == null) {
            return base;
         }

         base.setScaler(loaded.getScaler() != null ? loaded.getScaler() : base.getScaler());
         base.setOffset(loaded.getOffset() != null ? loaded.getOffset() : base.getOffset());
         if (loaded.getExpd_demoMode() != null) {
            base.setExpd_demoMode(loaded.getExpd_demoModeValue());
         }

         if (loaded.getWgen_wrapMode() != null) {
            base.setWgen_wrapMode(loaded.getWgen_wrapModeValue());
         }

         if (loaded.getWgen_modifyMode() != null) {
            base.setWgen_modifyMode(loaded.getWgen_modifyModeValue());
         }

         if (loaded.getWgen_lerpMode() != null) {
            base.setWgen_lerpMode(loaded.getWgen_lerpModeValue());
         }

         if (loaded.getExpd_debugOverlay() != null) {
            base.setExpd_debugOverlay(loaded.getExpd_debugOverlayValue());
         }

         if (loaded.getExpd_reportWorldAsDebugWorld() != null) {
            base.setExpd_reportWorldAsDebugWorld(loaded.getExpd_reportWorldAsDebugWorldValue());
         }

         if (loaded.getWgen_amplitude() != null) {
            base.setWgen_amplitude(loaded.getWgen_amplitudeValue());
         }

         if (loaded.getWgen_frequency() != null) {
            base.setWgen_frequency(loaded.getWgen_frequencyValue());
         }

         if (loaded.getWgen_lerpRate() != null) {
            base.setWgen_lerpRate(loaded.getWgen_lerpRateValue());
         }

         if (loaded.getWgen_wrapDivision() != null) {
            base.setWgen_wrapDivision(loaded.getWgen_wrapDivisionValue());
         }

         if (loaded.getExpd_enableEnhancedPauseScreen() != null) {
            base.setExpd_enableEnhancedPauseScreen(loaded.getExpd_enableEnhancedPauseScreenValue());
         }

         if (loaded.getExpd_enableToolBar() != null) {
            base.setExpd_enableToolBar(loaded.getExpd_EnableToolBarValue());
         }

         if (loaded.getEnpd_enableOperateMenu() != null) {
            base.setExpd_enableOperateMenu(loaded.getExpd_enableOperateMenuValue());
         }

         if (loaded.getWgen_minLimitNoiseCounterDivision() != null) {
            base.setWgen_minLimitNoiseCounterDivision(loaded.getWgen_minLimitNoiseCounterDivisionValue());
         }

         if (loaded.getWgen_maxLimitNoiseCounterDivision() != null) {
            base.setWgen_maxLimitNoiseCounterDivision(loaded.getWgen_maxLimitNoiseCounterDivisionValue());
         }

         return base;
      } catch (Exception e) {
         return new TppSettings();
      }
   }

   public Vec3Options<Double> getScaler() {
      return this.wgen_scale;
   }

   public double[] getScalerAsArray() {
      return new double[]{this.wgen_scale.x, this.wgen_scale.y, this.wgen_scale.z};
   }

   public List<SettingItem<Double>> getScalerAsItemArray() {
      return this.wgen_scale.toItemArray();
   }

   public double getScalerX() {
      return this.wgen_scale.x;
   }

   public double getScalerY() {
      return this.wgen_scale.y;
   }

   public double getScalerZ() {
      return this.wgen_scale.z;
   }

   public void setScalerX(double x) {
      this.wgen_scale.x = x;
   }

   public void setScalerY(double y) {
      this.wgen_scale.y = y;
   }

   public void setScalerZ(double z) {
      this.wgen_scale.z = z;
   }

   public void setScaler(Vec3Options<Double> scaler) {
      this.wgen_scale = scaler;
   }

   public void setScaler(double x, double y, double z) {
      this.wgen_scale = new Vec3Options<>(x, y, z);
   }

   public Vec3Options<Double> getOffset() {
      return this.wgen_offset;
   }

   public double getOffsetX() {
      return this.wgen_offset.x;
   }

   public double getOffsetY() {
      return this.wgen_offset.y;
   }

   public double getOffsetZ() {
      return this.wgen_offset.z;
   }

   public void setOffsetX(double x) {
      this.wgen_offset.x = x;
   }

   public void setOffsetY(double y) {
      this.wgen_offset.y = y;
   }

   public void setOffsetZ(double z) {
      this.wgen_offset.z = z;
   }

   public double[] getOffsetAsArray() {
      return new double[]{this.wgen_offset.x, this.wgen_offset.y, this.wgen_offset.z};
   }

   public List<SettingItem<Double>> getOffsetAsItemArray() {
      return this.wgen_offset.toItemArray();
   }

   public void setOffset(Vec3Options<Double> offset) {
      this.wgen_offset = offset;
   }

   public void setOffset(double x, double y, double z) {
      this.wgen_offset = new Vec3Options<>(x, y, z);
   }

   public SettingItem<Boolean> getExpd_demoMode() {
      return this.expd_demoMode;
   }

   public boolean getExpd_demoModeValue() {
      return this.expd_demoMode.getValue();
   }

   public void setExpd_demoMode(SettingItem<Boolean> expd_demoMode) {
      this.expd_demoMode = expd_demoMode;
   }

   public void setExpd_demoMode(boolean demoMode) {
      this.expd_demoMode.setValue(demoMode);
   }

   public SettingItem<WrapMode> getWgen_wrapMode() {
      return this.wgen_wrapMode;
   }

   public WrapMode getWgen_wrapModeValue() {
      return this.wgen_wrapMode.getValue();
   }

   public void setWgen_wrapMode(SettingItem<WrapMode> wgen_wrapMode) {
      this.wgen_wrapMode = wgen_wrapMode;
   }

   public void setWgen_wrapMode(WrapMode wrapMode) {
      this.wgen_wrapMode.setValue(wrapMode);
   }

   public SettingItem<NoiseModifyMode> getWgen_modifyMode() {
      return this.wgen_modifyMode;
   }

   public NoiseModifyMode getWgen_modifyModeValue() {
      return this.wgen_modifyMode.getValue();
   }

   public void setWgen_modifyMode(SettingItem<NoiseModifyMode> wgen_modifyMode) {
      this.wgen_modifyMode = wgen_modifyMode;
   }

   public void setWgen_modifyMode(NoiseModifyMode noiseModifyMode) {
      this.wgen_modifyMode.setValue(noiseModifyMode);
   }

   public SettingItem<LerpMode> getWgen_lerpMode() {
      return this.wgen_lerpMode;
   }

   public LerpMode getWgen_lerpModeValue() {
      return this.wgen_lerpMode.getValue();
   }

   public void setWgen_lerpMode(SettingItem<LerpMode> wgen_lerpMode) {
      this.wgen_lerpMode = wgen_lerpMode;
   }

   public void setWgen_lerpMode(LerpMode lerpMode) {
      this.wgen_lerpMode.setValue(lerpMode);
   }

   public SettingItem<Boolean> getExpd_debugOverlay() {
      return this.expd_debugOverlay;
   }

   public boolean getExpd_debugOverlayValue() {
      return this.expd_debugOverlay.getValue();
   }

   public void setExpd_debugOverlay(SettingItem<Boolean> expd_debugOverlay) {
      this.expd_debugOverlay = expd_debugOverlay;
   }

   public void setExpd_debugOverlay(boolean debugOverlay) {
      this.expd_debugOverlay.setValue(debugOverlay);
   }

   public SettingItem<LevelTypeReportMode> getExpd_reportWorldAsDebugWorld() {
      return this.expd_reportWorldAsDebugWorld;
   }

   public LevelTypeReportMode getExpd_reportWorldAsDebugWorldValue() {
      return this.expd_reportWorldAsDebugWorld.getValue();
   }

   public void setExpd_reportWorldAsDebugWorld(SettingItem<LevelTypeReportMode> expd_reportWorldAsDebugWorld) {
      this.expd_reportWorldAsDebugWorld = expd_reportWorldAsDebugWorld;
   }

   public void setExpd_reportWorldAsDebugWorld(LevelTypeReportMode reportWorldAsDebugWorld) {
      this.expd_reportWorldAsDebugWorld.setValue(reportWorldAsDebugWorld);
   }

   public SettingItem<Double> getWgen_amplitude() {
      return this.wgen_amplitude;
   }

   public double getWgen_amplitudeValue() {
      return this.wgen_amplitude.getValue();
   }

   public void setWgen_amplitude(SettingItem<Double> wgen_amplitude) {
      this.wgen_amplitude = wgen_amplitude;
   }

   public void setWgen_amplitude(double amplitude) {
      this.wgen_amplitude.setValue(amplitude);
   }

   public SettingItem<Double> getWgen_frequency() {
      return this.wgen_frequency;
   }

   public double getWgen_frequencyValue() {
      return this.wgen_frequency.getValue();
   }

   public void setWgen_frequency(SettingItem<Double> wgen_frequency) {
      this.wgen_frequency = wgen_frequency;
   }

   public void setWgen_frequency(double frequency) {
      this.wgen_frequency.setValue(frequency);
   }

   public SettingItem<Double> getWgen_lerpRate() {
      return this.wgen_lerpRate;
   }

   public double getWgen_lerpRateValue() {
      return this.wgen_lerpRate.getValue();
   }

   public void setWgen_lerpRate(SettingItem<Double> wgen_lerpRate) {
      this.wgen_lerpRate = wgen_lerpRate;
   }

   public void setWgen_lerpRate(double lerpRate) {
      this.wgen_lerpRate.setValue(lerpRate);
   }

   public SettingItem<Double> getWgen_wrapDivision() {
      return this.wgen_wrapDivision;
   }

   public double getWgen_wrapDivisionValue() {
      return this.wgen_wrapDivision.getValue();
   }

   public void setWgen_wrapDivision(SettingItem<Double> wgen_wrapDivision) {
      this.wgen_wrapDivision = wgen_wrapDivision;
   }

   public void setWgen_wrapDivision(double wrapDivision) {
      this.wgen_wrapDivision.setValue(wrapDivision);
   }

   public SettingItem<Boolean> getExpd_enableEnhancedPauseScreen() {
      return this.expd_enableEnhancedPauseScreen;
   }

   public boolean getExpd_enableEnhancedPauseScreenValue() {
      return this.getExpd_enableEnhancedPauseScreen().getValue();
   }

   public void setExpd_enableEnhancedPauseScreen(boolean b) {
      this.getExpd_enableEnhancedPauseScreen().setValue(b);
   }

   public void setExpd_enableEnhancedPauseScreen(SettingItem<Boolean> enableEnhancedPauseScreen1) {
      this.expd_enableEnhancedPauseScreen = enableEnhancedPauseScreen1;
   }

   public SettingItem<Boolean> getExpd_enableToolBar() {
      return this.expd_enableToolBar;
   }

   public boolean getExpd_EnableToolBarValue() {
      return this.getExpd_enableToolBar().getValue();
   }

   public void setExpd_enableToolBar(SettingItem<Boolean> settingItem) {
      this.expd_enableToolBar = settingItem;
   }

   public void setExpd_enableToolBar(boolean b) {
      this.getExpd_enableToolBar().setValue(b);
   }

   public SettingItem<Boolean> getEnpd_enableOperateMenu() {
      return this.enpd_enableOperateMenu;
   }

   public boolean getExpd_enableOperateMenuValue() {
      return this.enpd_enableOperateMenu.getValue();
   }

   public void setEnpd_enableOperateMenu(SettingItem<Boolean> optionInstance) {
      this.enpd_enableOperateMenu = optionInstance;
   }

   public void setExpd_enableOperateMenu(boolean b) {
      this.enpd_enableOperateMenu.setValue(b);
   }

   public SettingItem<Double> getWgen_minLimitNoiseCounterDivision() {
      return this.wgen_minLimitNoiseCounterDivision;
   }

   public double getWgen_minLimitNoiseCounterDivisionValue() {
      return this.getWgen_minLimitNoiseCounterDivision().getValue();
   }

   public void setWgen_minLimitNoiseCounterDivision(SettingItem<Double> maxLimitNoiseCounterDivision) {
      this.wgen_minLimitNoiseCounterDivision = maxLimitNoiseCounterDivision;
   }

   public void setWgen_minLimitNoiseCounterDivision(double minLimitNoiseCounterDivision) {
      this.getWgen_minLimitNoiseCounterDivision().setValue(minLimitNoiseCounterDivision);
   }

   public SettingItem<Double> getWgen_maxLimitNoiseCounterDivision() {
      return this.wgen_maxLimitNoiseCounterDivision;
   }

   public double getWgen_maxLimitNoiseCounterDivisionValue() {
      return this.getWgen_maxLimitNoiseCounterDivision().getValue();
   }

   public void setWgen_maxLimitNoiseCounterDivision(SettingItem<Double> wgen_maxLimitNoiseCounterDivision) {
      this.wgen_maxLimitNoiseCounterDivision = wgen_maxLimitNoiseCounterDivision;
   }

   public void setWgen_maxLimitNoiseCounterDivision(double maxLimitNoiseCounterDivision) {
      this.getWgen_maxLimitNoiseCounterDivision().setValue(maxLimitNoiseCounterDivision);
   }

   public SettingItem<Boolean> getExpd_noRealmsErrorScreen() {
      return this.expd_noRealmsErrorScreen;
   }

   public boolean getExpd_noRealmsErrorScreenValue() {
      return this.getExpd_noRealmsErrorScreen().getValue();
   }

   public void setExpd_noRealmsErrorScreen(SettingItem<Boolean> expd_noRealmsErrorScreen) {
      this.expd_noRealmsErrorScreen = expd_noRealmsErrorScreen;
   }

   public void setExpd_noRealmsErrorScreen(boolean b) {
      this.getExpd_noRealmsErrorScreen().setValue(b);
   }
}
