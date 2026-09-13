package com.mojang.renderpearl.backend.opengl;

import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.device.DeviceFeatures;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.DeviceLimits;
import com.mojang.renderpearl.api.device.DeviceType;
import com.mojang.renderpearl.api.device.HintsAndWorkarounds;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

public class GlHeuristics {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final List<String> DEVICE_NAMES_THAT_IMPLY_CPU = List.of("mesa offscreen", "llvmpipe");
   private static final List<String> DEVICE_NAMES_THAT_IMPLY_VIRTUAL = List.of("virtgl");
   private final boolean isGlOnDx12;
   private final boolean isAmd;

   GlHeuristics(final String deviceName) {
      this.isGlOnDx12 = isGlOnDx12(deviceName);
      this.isAmd = isAmd(deviceName);
   }

   public boolean isGlOnDx12() {
      return this.isGlOnDx12;
   }

   public boolean isAmd() {
      return this.isAmd;
   }

   private static boolean isGlOnDx12(final String deviceName) {
      boolean isWindowsArm64 = Util.getPlatform() == Util.OS.WINDOWS && Util.isAarch64();
      return isWindowsArm64 || deviceName.startsWith("D3D12");
   }

   private static boolean isAmd(final String deviceName) {
      return deviceName.contains("AMD");
   }

   private static int getMaxSupportedTextureSize() {
      int maxReported = GlStateManager._getInteger(3379);

      for (int texSize = Math.max(32768, maxReported); texSize >= 1024; texSize >>= 1) {
         GlStateManager._texImage2D(32868, 0, 6408, texSize, texSize, 0, 6408, 5121, null);
         int width = GlStateManager._getTexLevelParameter(32868, 0, 4096);
         if (width != 0) {
            return texSize;
         }
      }

      int maxSupportedTextureSize = Math.max(maxReported, 1024);
      LOGGER.info("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = {}", maxSupportedTextureSize);
      return maxSupportedTextureSize;
   }

   public DeviceInfo createDeviceInfo(final GLCapabilities capabilities, final int maxSupportedAnisotropy, final Set<String> enabledExtensions) {
      String renderer = GlStateManager._getString(7937);
      String vendor = GlStateManager._getString(7936);
      return new DeviceInfo(
         renderer,
         vendor,
         GlStateManager._getString(7938),
         capabilities.GL_ARB_clip_control,
         "OpenGL",
         1.0F,
         new DeviceLimits(maxSupportedAnisotropy, GL33C.glGetInteger(35380), getMaxSupportedTextureSize(), Long.MAX_VALUE, 0, GL33C.glGetInteger(34852)),
         new DeviceFeatures(
            enabledExtensions.contains("GL_ARB_shader_draw_parameters"),
            false,
            true,
            enabledExtensions.contains("GL_ARB_multi_draw_indirect"),
            enabledExtensions.contains("GL_ARB_draw_indirect"),
            enabledExtensions.contains("GL_ARB_base_instance"),
            enabledExtensions.contains("GL_ARB_buffer_storage")
         ),
         Collections.unmodifiableSet(enabledExtensions),
         new HintsAndWorkarounds(this.isGlOnDx12(), this.isAmd()),
         this.guessDeviceType(renderer.toLowerCase(Locale.ROOT), vendor.toLowerCase(Locale.ROOT))
      );
   }

   private DeviceType guessDeviceType(final String renderer, final String vendor) {
      if (vendor.contains("intel")) {
         return renderer.contains("arc") ? DeviceType.DISCRETE : DeviceType.INTEGRATED;
      }

      for (String string : DEVICE_NAMES_THAT_IMPLY_CPU) {
         if (renderer.contains(string)) {
            return DeviceType.CPU;
         }
      }

      for (String string : DEVICE_NAMES_THAT_IMPLY_VIRTUAL) {
         if (renderer.contains(string)) {
            return DeviceType.VIRTUAL;
         }
      }

      return DeviceType.OTHER;
   }
}
