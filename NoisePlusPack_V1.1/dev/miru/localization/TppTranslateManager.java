package dev.miru.localization;

import java.util.HashMap;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public class TppTranslateManager {
   private String languageCode = "en_us";
   private static final HashMap<String, HashMap<String, String>> translations = new HashMap<>();
   private static final HashMap<String, String> displayNames = new HashMap<>();

   public TppTranslateManager() {
      Minecraft minecraft = Minecraft.getInstance();
      if (this.isSupportedLanguage(minecraft.options.languageCode)) {
         this.languageCode = minecraft.options.languageCode;
      }
   }

   public static HashMap<String, HashMap<String, String>> getTranslations() {
      return translations;
   }

   public String getLanguageCode() {
      return this.languageCode;
   }

   public void setLanguageCode(String languageCode) {
      this.languageCode = languageCode;
   }

   public String getI18N(String key, Object... args) {
      HashMap<String, String> languageMap = translations.getOrDefault(this.languageCode, translations.get("en-us"));
      String value = languageMap != null ? languageMap.getOrDefault(key, key) : key;
      if (args != null && args.length != 0) {
         try {
            return String.format(Locale.ROOT, value, args);
         } catch (Exception e) {
            return value;
         }
      } else {
         return value;
      }
   }

   public String getI18N(String key) {
      return this.getI18N(key);
   }

   public boolean isSupportedLanguage(String languageCode) {
      return translations.containsKey(languageCode);
   }

   public void syncGameLanguage(Minecraft minecraft) {
      String gameLanguage = minecraft.getLanguageManager().getSelected();
      if (this.isSupportedLanguage(gameLanguage)) {
         this.setLanguageCode(gameLanguage);
      } else {
         System.out.printf("[TPP Language Manager]Unsupported language : %s%n", gameLanguage);
      }
   }

   public HashMap<String, String> getDisplayNames() {
      return displayNames;
   }

   static {
      HashMap<String, String> en_us = new HashMap<>();
      HashMap<String, String> zh_cn = new HashMap<>();
      displayNames.put("en_us", "English (US)");
      displayNames.put("zh_cn", "中文 (简体)");
      en_us.put("menu.play", "Play");
      zh_cn.put("menu.play", "开始");
      en_us.put("title.mod.settings.guide.title", "TPP Menu");
      zh_cn.put("title.mod.settings.guide.title", "TPP 菜单");
      en_us.put("common.switch.on", "ON");
      zh_cn.put("common.switch.on", "开");
      en_us.put("common.switch.off", "OFF");
      zh_cn.put("common.switch.off", "关");
      en_us.put("common.axis.x", "X");
      zh_cn.put("common.axis.x", "X");
      en_us.put("common.axis.y", "Y");
      zh_cn.put("common.axis.y", "Y");
      en_us.put("common.axis.z", "Z");
      zh_cn.put("common.axis.z", "Z");
      en_us.put("options.demo.mode.desc", "Demo mode: %s");
      zh_cn.put("options.demo.mode.desc", "演示模式: %s");
      en_us.put("options.advanced_arguments.title", "Advanced arguments");
      zh_cn.put("options.advanced_arguments.title", "高级参数");
      en_us.put("options.amplitude.title", "Amplitude");
      zh_cn.put("options.amplitude.title", "振幅");
      en_us.put("options.frequency.title", "Frequency");
      zh_cn.put("options.frequency.title", "频率");
      en_us.put("options.noise.modify.mode.desc", "Noise modify mode : %s");
      zh_cn.put("options.noise.modify.mode.desc", "噪声修正模式 : %s");
      en_us.put("options.noise.modify.mode.scale_offset.display_name", "Scale, then offset");
      zh_cn.put("options.noise.modify.mode.scale_offset.display_name", "先缩放，再偏移");
      en_us.put("options.noise.modify.mode.scale_offset.hint", "Apply scale, then apply offset.");
      zh_cn.put("options.noise.modify.mode.scale_offset.hint", "先缩放，再偏移。");
      en_us.put("options.noise.modify.mode.offset_scale.display_name", "Offset, then scale");
      zh_cn.put("options.noise.modify.mode.offset_scale.display_name", "先偏移，再缩放");
      en_us.put("options.noise.modify.mode.offset_scale.hint", "Apply offset, then apply scale.");
      zh_cn.put("options.noise.modify.mode.offset_scale.hint", "先偏移，再缩放。");
      en_us.put("options.noise.modify.mode.scale_only.display_name", "Scale only");
      zh_cn.put("options.noise.modify.mode.scale_only.display_name", "仅缩放");
      en_us.put("options.noise.modify.mode.scale_only.hint", "Only apply scale.");
      zh_cn.put("options.noise.modify.mode.scale_only.hint", "仅应用缩放。");
      en_us.put("options.noise.modify.mode.offset_only.display_name", "Offset only");
      zh_cn.put("options.noise.modify.mode.offset_only.display_name", "仅偏移");
      en_us.put("options.noise.modify.mode.offset_only.hint", "Only apply offset.");
      zh_cn.put("options.noise.modify.mode.offset_only.hint", "仅应用偏移。");
      en_us.put("options.lerp.mode.desc", "Lerp mode : %s");
      zh_cn.put("options.lerp.mode.desc", "插值模式 : %s");
      en_us.put("options.lerp.rate.title", "Lerp rate");
      zh_cn.put("options.lerp.rate.title", "插值速率");
      en_us.put("options.lerp.mode.start_div.display_name", "Start(def-div)");
      zh_cn.put("options.lerp.mode.start_div.display_name", "起始(默认除法)");
      en_us.put("options.lerp.mode.start_cdiv.display_name", "Start(cus-div)");
      zh_cn.put("options.lerp.mode.start_cdiv.display_name", "起始(自定义除法)");
      en_us.put("options.lerp.mode.start_ndiv.display_name", "Start(no-div)");
      zh_cn.put("options.lerp.mode.start_ndiv.display_name", "起始(不除法)");
      en_us.put("options.lerp.mode.start_div.hint", "Use start value for interpolation, and divide it by lerp rate (default 128)");
      zh_cn.put("options.lerp.mode.start_div.hint", "使用起始值进行插值，并按插值速率除以该值（默认 128）");
      en_us.put("options.lerp.mode.start_cdiv.hint", "Use start value for interpolation, and divide it by custom lerp rate");
      zh_cn.put("options.lerp.mode.start_cdiv.hint", "使用起始值进行插值，并按自定义插值速率除以该值");
      en_us.put("options.lerp.mode.start_ndiv.hint", "Use start value for interpolation, and do not divide it");
      zh_cn.put("options.lerp.mode.start_ndiv.hint", "使用起始值进行插值，并且不做除法处理");
      en_us.put("options.lerp.mode.end_div.display_name", "End(def-div)");
      zh_cn.put("options.lerp.mode.end_div.display_name", "结束(默认除法)");
      en_us.put("options.lerp.mode.end_cdiv.display_name", "End(cus-div)");
      zh_cn.put("options.lerp.mode.end_cdiv.display_name", "结束(自定义除法)");
      en_us.put("options.lerp.mode.end_ndiv.display_name", "End(no-div)");
      zh_cn.put("options.lerp.mode.end_ndiv.display_name", "结束(不除法)");
      en_us.put("options.lerp.mode.end_div.hint", "Use end value for interpolation, and divide it by lerp rate (default 128)");
      zh_cn.put("options.lerp.mode.end_div.hint", "使用结束值进行插值，并按插值速率除以该值（默认 128）");
      en_us.put("options.lerp.mode.end_cdiv.hint", "Use end value for interpolation, and divide it by custom lerp rate");
      zh_cn.put("options.lerp.mode.end_cdiv.hint", "使用结束值进行插值，并按自定义插值速率除以该值");
      en_us.put("options.lerp.mode.end_ndiv.hint", "Use end value for interpolation, and do not divide it");
      zh_cn.put("options.lerp.mode.end_ndiv.hint", "使用结束值进行插值，并且不做除法处理");
      en_us.put("options.lerp.mode.norm_div.display_name", "Norm(def-div)");
      zh_cn.put("options.lerp.mode.norm_div.display_name", "标准(默认除法)");
      en_us.put("options.lerp.mode.norm_cdiv.display_name", "Norm(cus-div)");
      zh_cn.put("options.lerp.mode.norm_cdiv.display_name", "标准(自定义除法)");
      en_us.put("options.lerp.mode.norm_ndiv.display_name", "Norm(no-div)");
      zh_cn.put("options.lerp.mode.norm_ndiv.display_name", "标准(不除法)");
      en_us.put("options.lerp.mode.norm_div.hint", "Use normal interpolation, and divide it by lerp rate (default 128)");
      zh_cn.put("options.lerp.mode.norm_div.hint", "使用常规插值，并按插值速率除以该值（默认 128）");
      en_us.put("options.lerp.mode.norm_cdiv.hint", "Use normal interpolation, and divide it by custom lerp rate");
      zh_cn.put("options.lerp.mode.norm_cdiv.hint", "使用常规插值，并按自定义插值速率除以该值");
      en_us.put("options.lerp.mode.norm_ndiv.hint", "Use normal interpolation, and do not divide it");
      zh_cn.put("options.lerp.mode.norm_ndiv.hint", "使用常规插值，并且不做除法处理");
      en_us.put("options.lerp.mode.prog_div.display_name", "Prog(def-div)");
      zh_cn.put("options.lerp.mode.prog_div.display_name", "进度(默认除法)");
      en_us.put("options.lerp.mode.prog_cdiv.display_name", "Prog(cus-div)");
      zh_cn.put("options.lerp.mode.prog_cdiv.display_name", "进度(自定义除法)");
      en_us.put("options.lerp.mode.prog_ndiv.display_name", "Prog(no-div)");
      zh_cn.put("options.lerp.mode.prog_ndiv.display_name", "进度(不除法)");
      en_us.put("options.lerp.mode.prog_div.hint", "Use progress for interpolation, and divide it by lerp rate (default 128)");
      zh_cn.put("options.lerp.mode.prog_div.hint", "使用进度值进行插值，并按插值速率除以该值（默认 128）");
      en_us.put("options.lerp.mode.prog_cdiv.hint", "Use progress for interpolation, and divide it by custom lerp rate");
      zh_cn.put("options.lerp.mode.prog_cdiv.hint", "使用进度值进行插值，并按自定义插值速率除以该值");
      en_us.put("options.lerp.mode.prog_ndiv.hint", "Use progress for interpolation, and do not divide it");
      zh_cn.put("options.lerp.mode.prog_ndiv.hint", "使用进度值进行插值，并且不做除法处理");
      en_us.put("options.wrap.mode.title", "Wrap mode");
      zh_cn.put("options.wrap.mode.title", "包裹模式");
      en_us.put("options.wrap.division.title", "Wrap division");
      zh_cn.put("options.wrap.division.title", "包裹分割");
      en_us.put("options.wrap.mode.vanilla.display_name", "Vanilla");
      zh_cn.put("options.wrap.mode.vanilla.display_name", "原版");
      en_us.put("options.wrap.mode.cus_div.display_name", "Cus-Div");
      zh_cn.put("options.wrap.mode.cus_div.display_name", "自定义分割");
      en_us.put("options.wrap.mode.raw_pos.display_name", "Raw-Pos");
      zh_cn.put("options.wrap.mode.raw_pos.display_name", "原始位置");
      en_us.put("options.wrap.mode.vanilla.hint", "Use vanilla wrap logic.");
      zh_cn.put("options.wrap.mode.vanilla.hint", "使用原版包裹逻辑。");
      en_us.put("options.wrap.mode.cus_div.hint", "Use vanilla logic, with custom division.");
      zh_cn.put("options.wrap.mode.cus_div.hint", "使用原版逻辑，并带有自定义分割。");
      en_us.put("options.wrap.mode.raw_pos.hint", "Return original position.");
      zh_cn.put("options.wrap.mode.raw_pos.hint", "返回原始位置。");
      en_us.put("title.terrain.arguments", "Terrain arguments");
      zh_cn.put("title.terrain.arguments", "地形参数");
      en_us.put("title.options.import_export", "Import & export options");
      zh_cn.put("title.options.import_export", "导入与导出选项");
      en_us.put("options.save", "Save");
      zh_cn.put("options.save", "保存");
      en_us.put("options.cancel", "Cancel");
      zh_cn.put("options.cancel", "取消");
      en_us.put("options.import", "Import");
      zh_cn.put("options.import", "导入");
      en_us.put("options.export", "Export");
      zh_cn.put("options.export", "导出");
      en_us.put("options.reset", "Reset");
      zh_cn.put("options.reset", "重置");
      en_us.put("options.import.hint", "Import options as INI by the box above.");
      zh_cn.put("options.import.hint", "从上方文本框导入 INI 选项。");
      en_us.put("options.export.hint", "Export options as INI by the box above.");
      zh_cn.put("options.export.hint", "将选项导出为上方文本框中的 INI。");
      en_us.put("options.import_export.hint", "Import or export options, saved by INI string.");
      zh_cn.put("options.import_export.hint", "通过 INI 字符串导入或导出选项。");
      en_us.put("options.reset.hint", "Reset all options to default.");
      zh_cn.put("options.reset.hint", "将所有选项重置为默认值。");
      en_us.put("options.noise.scale", "Noise scale %s");
      zh_cn.put("options.noise.scale", "噪声缩放 %s");
      en_us.put("options.noise.offset", "Noise offset %s");
      zh_cn.put("options.noise.offset", "噪声偏移 %s");
      en_us.put("subtitle.noise.scale", "Noise Scale");
      zh_cn.put("subtitle.noise.scale", "噪声缩放");
      en_us.put("subtitle.noise.offset", "Noise Offset");
      zh_cn.put("subtitle.noise.offset", "噪声偏移");
      en_us.put("options.debug.overlay.desc", "Debug overlay : %s");
      zh_cn.put("options.debug.overlay.desc", "调试覆盖层 : %s");
      en_us.put("options.report_as_debug_world.desc", "Report level as debug : %s");
      zh_cn.put("options.report_as_debug_world.desc", "将当前存档上报为调试 : %s");
      en_us.put("options.report_as_debug_world.always.display_name", "Always");
      zh_cn.put("options.report_as_debug_world.always.display_name", "总是");
      en_us.put("options.report_as_debug_world.always.hint", "Report current level as debug world.");
      zh_cn.put("options.report_as_debug_world.always.hint", "将当前层级上报为调试世界。");
      en_us.put("options.report_as_debug_world.never.display_name", "Never");
      zh_cn.put("options.report_as_debug_world.never.display_name", "从不");
      en_us.put("options.report_as_debug_world.never.hint", "Never report current level as debug world.");
      zh_cn.put("options.report_as_debug_world.never.hint", "永不将当前层级上报为调试世界。");
      en_us.put("options.report_as_debug_world.detect.display_name", "Detect");
      zh_cn.put("options.report_as_debug_world.detect.display_name", "检测");
      en_us.put("options.report_as_debug_world.detect.hint", "Detect if current level is a debug world.");
      zh_cn.put("options.report_as_debug_world.detect.hint", "检测当前层级是否为调试世界。");
      en_us.put("toolbar.open.singleplayer.title", "Open singleplayer menu");
      zh_cn.put("toolbar.open.singleplayer.title", "打开单人游戏菜单");
      en_us.put("toolbar.open.multiplayer.title", "Open multiplayer menu");
      zh_cn.put("toolbar.open.multiplayer.title", "打开多人游戏菜单");
      en_us.put("toolbar.open.options.title", "Open options menu");
      zh_cn.put("toolbar.open.options.title", "打开选项菜单");
      en_us.put("toolbar.create.world.title", "Create new world");
      zh_cn.put("toolbar.create.world.title", "创建新世界");
      en_us.put("toolbar.throw.test.title", "Throw test");
      zh_cn.put("toolbar.throw.test.title", "抛出测试");
      en_us.put("toolbar.throw.test.hint", "Throw exceptions.");
      zh_cn.put("toolbar.throw.test.hint", "抛出异常。");
      en_us.put("toolbar.pos.locator.title", "Pos Locator");
      zh_cn.put("toolbar.pos.locator.title", "位置定位器");
      en_us.put("toolbar.pos.locator.hint", "Locate a target position generated pos in world.");
      zh_cn.put("toolbar.pos.locator.hint", "定位世界中生成的目标位置。");
      en_us.put("screen.quick_link.title", "Quick link");
      zh_cn.put("screen.quick_link.title", "快捷链接");
      en_us.put("screen.quick_link.open_debug_overlay_editor", "Open Debug Overlay Editor");
      zh_cn.put("screen.quick_link.open_debug_overlay_editor", "打开调试覆盖层编辑器");
      en_us.put("screen.quick_link.access_key", "Assess key : F3 + F6 (In default settings)");
      zh_cn.put("screen.quick_link.access_key", "访问键：F3 + F6（默认设置）");
      en_us.put("screen.quick_link.open_multiplayer_safety", "Open Multiplayer Safety Screen");
      zh_cn.put("screen.quick_link.open_multiplayer_safety", "打开多人游戏安全屏幕");
      en_us.put(
         "screen.quick_link.hide_after_trigger",
         "The screen will be hidden after you triggered \"Don't show this screen\" into true in multiplayer safety screen."
      );
      zh_cn.put("screen.quick_link.hide_after_trigger", "在多人游戏安全屏幕中将“不要再显示此屏幕”设置为开启后，此界面将被隐藏。");
      en_us.put("screen.position_locator.title", "Position locator");
      zh_cn.put("screen.position_locator.title", "位置定位器");
      en_us.put("screen.position_locator.input.x.hint", "Type X position to rev apply.");
      zh_cn.put("screen.position_locator.input.x.hint", "输入 X 位置以重新应用。");
      en_us.put("screen.position_locator.input.y.hint", "Type Y position to rev apply.");
      zh_cn.put("screen.position_locator.input.y.hint", "输入 Y 位置以重新应用。");
      en_us.put("screen.position_locator.input.z.hint", "Type Z position to rev apply.");
      zh_cn.put("screen.position_locator.input.z.hint", "输入 Z 位置以重新应用。");
      en_us.put("screen.position_locator.get_terrain_position", "Get terrain position");
      zh_cn.put("screen.position_locator.get_terrain_position", "获取地形位置");
      en_us.put("screen.position_locator.get_terrain_position.hint", "Try to rev apply to get target position generated in world.");
      zh_cn.put("screen.position_locator.get_terrain_position.hint", "尝试重新应用以获取世界中生成的目标位置。");
      en_us.put("screen.position_locator.apply.desc", "Apply direction : %s");
      zh_cn.put("screen.position_locator.apply.desc", "应用方向 : %s");
      en_us.put("screen.position_locator.apply.get_modified", "Get modified");
      zh_cn.put("screen.position_locator.apply.get_modified", "正向");
      en_us.put("screen.position_locator.apply.get_original", "Get original");
      zh_cn.put("screen.position_locator.apply.get_original", "反向");
      en_us.put("screen.position_locator.back", "Back");
      zh_cn.put("screen.position_locator.back", "返回");
      en_us.put("screen.quick_teleport.target_x", "Target position X");
      zh_cn.put("screen.quick_teleport.target_x", "目标位置 X");
      en_us.put("screen.quick_teleport.target_y", "Target position Y");
      zh_cn.put("screen.quick_teleport.target_y", "目标位置 Y");
      en_us.put("screen.quick_teleport.target_z", "Target position Z");
      zh_cn.put("screen.quick_teleport.target_z", "目标位置 Z");
      en_us.put("screen.quick_teleport.go", "Go");
      zh_cn.put("screen.quick_teleport.go", "前往");
      en_us.put("screen.quick_teleport.teleport_x_only", "Teleport X only.");
      zh_cn.put("screen.quick_teleport.teleport_x_only", "仅传送 X。");
      en_us.put("screen.quick_teleport.teleport_y_only", "Teleport Y only.");
      zh_cn.put("screen.quick_teleport.teleport_y_only", "仅传送 Y。");
      en_us.put("screen.quick_teleport.teleport_z_only", "Teleport Z only.");
      zh_cn.put("screen.quick_teleport.teleport_z_only", "仅传送 Z。");
      en_us.put("screen.quick_teleport.to_position", "Go to position");
      zh_cn.put("screen.quick_teleport.to_position", "前往位置");
      en_us.put("screen.pause.tpp_menu", "TPP Menu");
      zh_cn.put("screen.pause.tpp_menu", "TPP 菜单");
      en_us.put("screen.exception_throw_test.error.caught", "Error caught : %s");
      zh_cn.put("screen.exception_throw_test.error.caught", "捕获到错误 : %s");
      en_us.put("screen.import_export.title", "Import & export options");
      zh_cn.put("screen.import_export.title", "导入与导出选项");
      en_us.put("screen.import_export.hint", "Import or export options, saved by INI string.");
      zh_cn.put("screen.import_export.hint", "通过 INI 字符串导入或导出选项。");
      en_us.put("screen.import_export.import", "Import");
      zh_cn.put("screen.import_export.import", "导入");
      en_us.put("screen.import_export.import.hint", "Import options as INI by the box above.");
      zh_cn.put("screen.import_export.import.hint", "从上方文本框导入 INI 选项。");
      en_us.put("screen.import_export.export", "Export");
      zh_cn.put("screen.import_export.export", "导出");
      en_us.put("screen.import_export.export.hint", "Export options as INI by the box above.");
      zh_cn.put("screen.import_export.export.hint", "将选项导出为上方文本框中的 INI。");
      en_us.put("screen.terrain_arguments.section.amplitude_frequency", "Amplitude and Frequency");
      zh_cn.put("screen.terrain_arguments.section.amplitude_frequency", "振幅与频率");
      en_us.put("screen.terrain_arguments.min_noise_counter_division", "Min noise counter division.");
      zh_cn.put("screen.terrain_arguments.min_noise_counter_division", "最小噪声计数分段。");
      en_us.put("screen.terrain_arguments.max_noise_counter_division", "Max noise counter division.");
      zh_cn.put("screen.terrain_arguments.max_noise_counter_division", "最大噪声计数分段。");
      en_us.put("options.min_noise_counter_division.desc", "Min noise counter div : %s");
      zh_cn.put("options.min_noise_counter_division.desc", "最小噪声计数分段 : %s");
      en_us.put("options.max_noise_counter_division.desc", "Max noise counter div : %s");
      zh_cn.put("options.max_noise_counter_division.desc", "最大噪声计数分段 : %s");
      en_us.put("options.wrap_division.desc", "Wrap division : %s");
      zh_cn.put("options.wrap_division.desc", "包裹分割 : %s");
      en_us.put("options.enable_enhanced_pause_screen.desc", "Enable enhanced pause screen : %s");
      zh_cn.put("options.enable_enhanced_pause_screen.desc", "启用增强暂停屏幕 : %s");
      en_us.put("options.enable_toolbar.desc", "Enable toolbar : %s");
      zh_cn.put("options.enable_toolbar.desc", "启用工具栏 : %s");
      en_us.put("options.enable_operate_menu.desc", "Enable operate menu : %s");
      zh_cn.put("options.enable_operate_menu.desc", "启用操作菜单 : %s");
      en_us.put("options.no_realms_error_screen.desc", "No realms error screen : %s");
      zh_cn.put("options.no_realms_error_screen.desc", "不显示 Realms 错误屏幕 : %s");
      en_us.put("toolbar.expand.title", "Expand tool bar");
      zh_cn.put("toolbar.expand.title", "展开工具栏");
      en_us.put("toolbar.fold.title", "Fold tool bar");
      zh_cn.put("toolbar.fold.title", "折叠工具栏");
      en_us.put("selector.world_generator_settings.title", "World Generator Settings");
      zh_cn.put("selector.world_generator_settings.title", "世界生成器设置");
      en_us.put("selector.world_generator_settings.hint", "Open generator options screen.");
      zh_cn.put("selector.world_generator_settings.hint", "打开生成器选项屏幕。");
      en_us.put("selector.advanced_options.title", "Advanced Options");
      zh_cn.put("selector.advanced_options.title", "高级选项");
      en_us.put("selector.advanced_options.hint", "Advanced options for TPP.");
      zh_cn.put("selector.advanced_options.hint", "TPP 的高级选项。");
      en_us.put("selector.position_and_teleport.title", "Position & Teleport");
      zh_cn.put("selector.position_and_teleport.title", "位置与传送");
      en_us.put("selector.position_and_teleport.hint", "Position and teleport tools.");
      zh_cn.put("selector.position_and_teleport.hint", "位置与传送工具。");
      en_us.put("selector.terrain_arguments.title", "Terrain Arguments");
      zh_cn.put("selector.terrain_arguments.title", "地形参数");
      en_us.put("selector.terrain_arguments.hint", "Scale, offset, divisions, rates, etc.");
      zh_cn.put("selector.terrain_arguments.hint", "缩放、偏移、分割、速率等。");
      en_us.put("selector.terrain_modes.title", "Terrain Modes");
      zh_cn.put("selector.terrain_modes.title", "地形模式");
      en_us.put("selector.terrain_modes.hint", "Modify modes, Lerp modes, wrap modes.");
      zh_cn.put("selector.terrain_modes.hint", "修改模式、插值模式、包裹模式。");
      en_us.put("selector.position_locator.title", "Position Locator");
      zh_cn.put("selector.position_locator.title", "位置定位器");
      en_us.put("selector.position_locator.hint", "Locate real generated position of a target terrain position.");
      zh_cn.put("selector.position_locator.hint", "定位目标地形位置的真实生成位置。");
      en_us.put("selector.teleport.title", "Teleport");
      zh_cn.put("selector.teleport.title", "传送");
      en_us.put("selector.teleport.hint", "Quick teleport player.");
      zh_cn.put("selector.teleport.hint", "快速传送玩家。");
      en_us.put("selector.no_player_available", "No player available!");
      zh_cn.put("selector.no_player_available", "没有可用玩家！");
      en_us.put("screen.options.more", "More options");
      zh_cn.put("screen.options.more", "更多选项");
      en_us.put("screen.general_settings.title", "TPP Settings");
      zh_cn.put("screen.general_settings.title", "TPP 设置");
      en_us.put("screen.general_settings.hint", "TPP setting menu.");
      zh_cn.put("screen.general_settings.hint", "TPP 设置菜单。");
      en_us.put("screen.general_settings.tools.title", "TPP tools");
      zh_cn.put("screen.general_settings.tools.title", "TPP 工具");
      en_us.put("screen.general_settings.tools.hint", "Import & export options, position locator, etc.");
      zh_cn.put("screen.general_settings.tools.hint", "导入与导出选项、位置定位器等。");
      en_us.put("screen.general_settings.reset.confirm", "Are you sure to reset settings?");
      zh_cn.put("screen.general_settings.reset.confirm", "确定要重置设置吗？");
      en_us.put("screen.general_settings.reset.irreversible", "It's unrecoverable!");
      zh_cn.put("screen.general_settings.reset.irreversible", "这将无法撤销！");
      en_us.put("screen.advanced_options.toggle_debug_hud", "Toggle debug hud.");
      zh_cn.put("screen.advanced_options.toggle_debug_hud", "切换调试 HUD。");
      en_us.put("screen.advanced_options.toggle_level_type_report", "Toggle level type report mode.");
      zh_cn.put("screen.advanced_options.toggle_level_type_report", "切换层级类型上报模式。");
      en_us.put("screen.advanced_options.toggle_enchanted_pause", "Toggle enchanted pause screen.");
      zh_cn.put("screen.advanced_options.toggle_enchanted_pause", "切换增强暂停屏幕。");
      en_us.put("screen.advanced_options.toggle_demo_mode", "Toggle demo mode.");
      zh_cn.put("screen.advanced_options.toggle_demo_mode", "切换演示模式。");
      en_us.put("screen.advanced_options.toggle_toolbar", "Toggle tool bar");
      zh_cn.put("screen.advanced_options.toggle_toolbar", "切换工具栏");
      en_us.put("screen.advanced_options.toggle_operate_menu", "Toggle operate menu.");
      zh_cn.put("screen.advanced_options.toggle_operate_menu", "切换操作菜单。");
      en_us.put("screen.advanced_options.disable_realms_error", "Disable realms \"Invalid session\" error screen.");
      zh_cn.put("screen.advanced_options.disable_realms_error", "禁用 Realms \"无效会话\" 错误屏幕。");
      en_us.put("screen.advanced_options.quick_link", "Quick link");
      zh_cn.put("screen.advanced_options.quick_link", "快捷链接");
      en_us.put("screen.advanced_options.quick_link.hint", "Open screens which couldn't directly or once-only.");
      zh_cn.put("screen.advanced_options.quick_link.hint", "打开无法直接访问或只能访问一次的界面。");
      en_us.put("screen.advanced_options.back_last_screen", "Back to last screen.");
      zh_cn.put("screen.advanced_options.back_last_screen", "返回上一屏幕。");
      en_us.put("screen.exception_throw_test.title", "Exception throw test");
      zh_cn.put("screen.exception_throw_test.title", "异常抛出测试");
      en_us.put("screen.exception_throw_test.input.full_name.hint", "Type full name. e.g. \"java.lang.NullPointerException\"");
      zh_cn.put("screen.exception_throw_test.input.full_name.hint", "输入完整类名，例如 \"java.lang.NullPointerException\"");
      en_us.put("screen.exception_throw_test.throw.tooltip", "throw %s");
      zh_cn.put("screen.exception_throw_test.throw.tooltip", "抛出 %s");
      en_us.put("screen.exception_throw_test.input.message.hint", "Type message. e.g. \"Test exception\"");
      zh_cn.put("screen.exception_throw_test.input.message.hint", "输入消息，例如 \"Test exception\"");
      en_us.put("screen.exception_throw_test.throw", "Throw");
      zh_cn.put("screen.exception_throw_test.throw", "抛出");
      en_us.put("screen.exception_throw_test.throw.empty_name", "Please type a name.");
      zh_cn.put("screen.exception_throw_test.throw.empty_name", "请输入名称。");
      en_us.put("screen.exception_throw_test.back", "Back");
      zh_cn.put("screen.exception_throw_test.back", "返回");
      en_us.put("screen.noise_mode_trigger.title", "Noise mode trigger");
      zh_cn.put("screen.noise_mode_trigger.title", "噪声模式编辑器");
      en_us.put("screen.language_switcher.title", "Switch language");
      zh_cn.put("screen.language_switcher.title", "切换语言");
      en_us.put("screen.language_switcher.sync_game", "Sync game language.");
      zh_cn.put("screen.language_switcher.sync_game", "跟随游戏设置");
      en_us.put("debugger.title", "[Terrain plus pack debugger]");
      zh_cn.put("debugger.title", "[Terrain plus pack 调试器]");
      en_us.put("debugger.subtitle.options", "Options");
      zh_cn.put("debugger.subtitle.options", "选项");
      en_us.put("debugger.subtitle.pos", "Position");
      zh_cn.put("debugger.subtitle.pos", "位置");
      en_us.put("debugger.pos_view.player", "Player pos : [%.3f, %.3f, %.3f]");
      zh_cn.put("debugger.pos_view.player", "玩家位置 : [%.3f, %.3f, %.3f]");
      en_us.put("debugger.pos_view.player.level_load", "Load a level to view player position.");
      zh_cn.put("debugger.pos_view.player.load_level", "加载存档以查看玩家位置.");
      en_us.put("debugger.pos_view.terrain", "Terrain pos : [%.3f, %.3f, %.3f]");
      zh_cn.put("debugger.pos_view.terrain", "地形位置 : [%.3f, %.3f, %.3f]");
      en_us.put("debugger.pos_view.terrain.load_level", "Load a level to view terrain position.");
      zh_cn.put("debugger.pos_view.terrain.load_level", "加载存档以查看地形坐标.");
      en_us.put("debugger.options.noise_modify_mode", "Modify mode : %s");
      zh_cn.put("debugger.options.noise_modify_mode", "编辑模式 : %s");
      en_us.put("debugger.options.scaler", "Scaler : [%s, %s, %s]");
      zh_cn.put("debugger.options.scaler", "缩放 : [%s, %s, %s]");
      en_us.put("debugger.options.offset", "Offset : [%s, %s, %s]");
      zh_cn.put("debugger.options.offset", "偏移 : [%s, %s, %s]");
      en_us.put("debugger.options.limit_noise_division.title", "Limit noise division");
      zh_cn.put("debugger.options.limit_noise_division.title", "限位噪声除数");
      en_us.put("debugger.options.limit_noise_division.min", "/Min limit : %s");
      zh_cn.put("debugger.options.limit_noise_division.min", "/最低限位 : %s");
      en_us.put("debugger.options.limit_noise_division.max", "/Max limit : %s");
      zh_cn.put("debugger.options.limit_noise_division.max", "/最大限位 : %s");
      en_us.put("debugger.options.amplitude", "Amplitude : %s");
      zh_cn.put("debugger.options.amplitude", "频率 : %s");
      en_us.put("debugger.options.frequency", "Frequency : %s");
      zh_cn.put("debugger.options.frequency", "振幅 : %s");
      en_us.put("debugger.options.wrapper.mode", "Wrapper/Mode : %s");
      zh_cn.put("debugger.options.wrapper.mode", "包裹模式 : %s");
      en_us.put("debugger.options.wrapper.division", "Wrapper/Division : %s");
      zh_cn.put("debugger.options.wrapper.division", "包裹除数 : %s");
      en_us.put("debugger.options.lerp.mode", "Lerp/Mode : %s");
      zh_cn.put("debugger.options.lerp.mode", "插值模式 : %s");
      en_us.put("debugger.options.lerp.rate", "Lerp/Rate : %s");
      zh_cn.put("debugger.options.lerp.rate", "插值倍率 : %s");
      en_us.put("faq.terrain.title", "Argument details");
      en_us.put("faq.terrain.args.scale_offset.title", "Scale & Offset Settings");
      en_us.put("faq.terrain.args.scale_offset.desc", "Scale : Position delta per block; Offset : Change original point.");
      en_us.put("faq.terrain.args.frequency.title", "Frequency Settings");
      en_us.put("faq.terrain.args.frequency.desc", "Decides how much improved noise stage 1 effect the world.");
      en_us.put("faq.terrain.args.amplitude.title", "Amplitude Settings");
      en_us.put("faq.terrain.args.amplitude.desc", "Decides how much improved noise stage 2 effect the world.");
      en_us.put("faq.terrain.args.lerp.rate.title", "Lerp Rate Settings");
      en_us.put("faq.terrain.args.lerp.rate.desc", "Division on terrain lerp result, can't be zero.");
      en_us.put("faq.terrain.args.wrapper.division.title", "Wrapper Division Settings");
      en_us.put("faq.terrain.args.wrapper.division.desc", "Decides how far position could reach, e.g. 33554432 -> [-16777216,16777216]");
      en_us.put("faq.terrain.args.limit.noise.division.title", "Min/Max Limit Noise Division Settings");
      en_us.put("faq.terrain.args.limit.noise.division.desc", "Division on min/max noise counter, can't be zero.");
      zh_cn.put("faq.terrain.title", "参数细节");
      zh_cn.put("faq.terrain.args.scale_offset.title", "缩放&偏移设置");
      zh_cn.put("faq.terrain.args.scale_offset.desc", "缩放:每方块的坐标偏移量;偏移:改变原点");
      zh_cn.put("faq.terrain.args.frequency.title", "振幅设置");
      zh_cn.put("faq.terrain.args.frequency.desc", "决定噪声在生成阶段1对世界的影响程度");
      zh_cn.put("faq.terrain.args.amplitude.title", "频率设置");
      zh_cn.put("faq.terrain.args.amplitude.desc", "决定噪声在生成阶段2对世界的影响程度");
      zh_cn.put("faq.terrain.args.lerp.rate.title", "插值倍率设置");
      zh_cn.put("faq.terrain.args.lerp.rate.desc", "地形插值结果的除数,不能为零.");
      zh_cn.put("faq.terrain.args.wrapper.division.title", "包装除数设置");
      zh_cn.put("faq.terrain.args.wrapper.division.desc", "决定坐标可到达范围,如:33554432对应[-16777216,16777216]");
      zh_cn.put("faq.terrain.args.limit.noise.division.title", "限位噪声除数设置");
      zh_cn.put("faq.terrain.args.limit.noise.division.desc", "限位噪声计数的除数,不能为零.");
      translations.put("en_us", en_us);
      translations.put("zh_cn", zh_cn);
   }
}
