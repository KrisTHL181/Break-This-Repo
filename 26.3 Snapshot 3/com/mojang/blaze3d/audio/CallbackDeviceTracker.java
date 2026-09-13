package com.mojang.blaze3d.audio;

import com.mojang.logging.LogUtils;
import java.util.HexFormat;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.SOFTSystemEventProcI;
import org.lwjgl.openal.SOFTSystemEvents;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public class CallbackDeviceTracker extends AbstractDeviceTracker {
   private static final Logger LOGGER = LogUtils.getLogger();
   private volatile boolean updateRequested;
   private static final int[] SUBSCRIBED_EVENT_TYPES = new int[]{6614, 6615, 6616};
   public static final HexFormat HEX_FORMAT = HexFormat.of();

   public CallbackDeviceTracker(final DeviceList deviceList) {
      super(deviceList);
   }

   @Override
   protected boolean isUpdateRequested() {
      return this.updateRequested;
   }

   @Override
   protected void discardUpdateRequest() {
      this.updateRequested = false;
   }

   public static boolean isSupported() {
      if (!ALC10.alcIsExtensionPresent(0L, "ALC_SOFT_system_events")) {
         return false;
      }

      for (int eventType : SUBSCRIBED_EVENT_TYPES) {
         if (!isSupportedForPlaybackDevice(eventType)) {
            return false;
         }
      }

      return true;
   }

   public static CallbackDeviceTracker createAndInstall(final DeviceList deviceList) {
      CallbackDeviceTracker result = new CallbackDeviceTracker(deviceList);
      SOFTSystemEvents.alcEventControlSOFT(SUBSCRIBED_EVENT_TYPES, true);
      SOFTSystemEvents.alcEventCallbackSOFT(result.createCallback(), 0L);
      return result;
   }

   private SOFTSystemEventProcI createCallback() {
      return (eventType, deviceType, device, messageLength, messagePtr, userParam) -> {
         String deviceTypeString = deviceTypeToString(deviceType);
         String message = MemoryUtil.memASCII(messagePtr, messageLength);
         switch (eventType) {
            case 6614:
               LOGGER.debug("Default {} device changed: {}", deviceTypeString, message);
               break;
            case 6615:
               LOGGER.debug("Added new {} device: {}", deviceTypeString, message);
               break;
            case 6616:
               LOGGER.debug("Removed {} device: {}", deviceTypeString, message);
         }

         if (deviceType == 6612) {
            this.updateRequested = true;
         }
      };
   }

   private static boolean isSupportedForPlaybackDevice(final int eventType) {
      int result = SOFTSystemEvents.alcEventIsSupportedSOFT(eventType, 6612);
      if (result == 0) {
         int error = ALC10.alcGetError(0L);
         LOGGER.warn("Failed to check event {}, error: {}", HEX_FORMAT.toHexDigits(eventType), HEX_FORMAT.toHexDigits(error));
         return false;
      } else {
         return result == 6617;
      }
   }

   private static String deviceTypeToString(final int deviceType) {
      return switch (deviceType) {
         case 6612 -> "playback";
         case 6613 -> "capture";
         default -> "unknown (0x" + HEX_FORMAT.toHexDigits(deviceType) + ")";
      };
   }
}
