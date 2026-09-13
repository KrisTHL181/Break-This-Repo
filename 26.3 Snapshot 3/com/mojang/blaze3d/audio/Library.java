package com.mojang.blaze3d.audio;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.nio.IntBuffer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

public class Library {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int NO_DEVICE = 0;
   private static final String NO_DEVICE_NAME = "(None)";
   private static final int DEFAULT_CHANNEL_COUNT = 30;
   private long currentDevice;
   private String currentDeviceName = "(None)";
   private long context;
   private boolean supportsDisconnections;
   private static final Library.ChannelPool EMPTY = new Library.ChannelPool() {
      @Override
      public @Nullable Channel acquire() {
         return null;
      }

      @Override
      public boolean release(final Channel channel) {
         return false;
      }

      @Override
      public void cleanup() {
      }

      @Override
      public int getMaxCount() {
         return 0;
      }

      @Override
      public int getUsedCount() {
         return 0;
      }
   };
   private Library.ChannelPool staticChannels = EMPTY;
   private Library.ChannelPool streamingChannels = EMPTY;
   private final Listener listener = new Listener();

   public void init(final @Nullable String preferredDevice, final DeviceList currentDevices, final boolean useHrtf) {
      this.currentDeviceName = "(None)";
      this.currentDevice = openDeviceOrFallback(preferredDevice, currentDevices.defaultDevice());
      this.currentDeviceName = queryDeviceName(this.currentDevice);
      this.supportsDisconnections = false;
      ALCCapabilities alcCapabilities = ALC.createCapabilities(this.currentDevice);
      if (OpenAlUtil.checkALCError(this.currentDevice, "Get capabilities")) {
         throw new IllegalStateException("Failed to get OpenAL capabilities");
      }

      if (!alcCapabilities.OpenALC11) {
         throw new IllegalStateException("OpenAL 1.1 not supported");
      }

      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer attr = this.createAttributes(stack, alcCapabilities.ALC_SOFT_HRTF && useHrtf);
         this.context = ALC10.alcCreateContext(this.currentDevice, attr);
      } catch (Throwable var10) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }
         }

         throw var10;
      }

      if (stack != null) {
         stack.close();
      }

      if (OpenAlUtil.checkALCError(this.currentDevice, "Create context")) {
         throw new IllegalStateException("Unable to create OpenAL context");
      }

      ALC10.alcMakeContextCurrent(this.context);
      int totalChannelCount = this.getChannelCount();
      int streamingChannelCount = Mth.clamp((int)Mth.sqrt(totalChannelCount), 2, 8);
      int staticChannelCount = Mth.clamp(totalChannelCount - streamingChannelCount, 8, 255);
      this.staticChannels = new Library.CountingChannelPool(staticChannelCount);
      this.streamingChannels = new Library.CountingChannelPool(streamingChannelCount);
      ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
      OpenAlUtil.checkALError("Initialization");
      if (!alCapabilities.AL_EXT_source_distance_model) {
         throw new IllegalStateException("AL_EXT_source_distance_model is not supported");
      }

      AL10.alEnable(512);
      if (!alCapabilities.AL_EXT_LINEAR_DISTANCE) {
         throw new IllegalStateException("AL_EXT_LINEAR_DISTANCE is not supported");
      }

      OpenAlUtil.checkALError("Enable per-source distance models");
      LOGGER.info("OpenAL initialized on device {}", this.currentDeviceName);
      this.supportsDisconnections = ALC10.alcIsExtensionPresent(this.currentDevice, "ALC_EXT_disconnect");
   }

   private IntBuffer createAttributes(final MemoryStack stack, final boolean enableHrtf) {
      int maxAttributes = 5;
      IntBuffer attr = stack.callocInt(11);
      int numHrtf = ALC10.alcGetInteger(this.currentDevice, 6548);
      if (numHrtf > 0) {
         attr.put(6546).put(enableHrtf ? 1 : 0);
         attr.put(6550).put(0);
      }

      attr.put(6554).put(1);
      return attr.put(0).flip();
   }

   private int getChannelCount() {
      MemoryStack stack = MemoryStack.stackPush();

      int var7;
      label58: {
         try {
            int size = ALC10.alcGetInteger(this.currentDevice, 4098);
            if (OpenAlUtil.checkALCError(this.currentDevice, "Get attributes size")) {
               throw new IllegalStateException("Failed to get OpenAL attributes");
            }

            IntBuffer attributes = stack.mallocInt(size);
            ALC10.alcGetIntegerv(this.currentDevice, 4099, attributes);
            if (OpenAlUtil.checkALCError(this.currentDevice, "Get attributes")) {
               throw new IllegalStateException("Failed to get OpenAL attributes");
            }

            int pos = 0;

            while (pos < size) {
               int attribute = attributes.get(pos++);
               if (attribute == 0) {
                  break;
               }

               int attributeValue = attributes.get(pos++);
               if (attribute == 4112) {
                  var7 = attributeValue;
                  break label58;
               }
            }
         } catch (Throwable var9) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (stack != null) {
            stack.close();
         }

         return 30;
      }

      if (stack != null) {
         stack.close();
      }

      return var7;
   }

   public @Nullable String currentDeviceName() {
      return this.currentDeviceName;
   }

   private static String queryDeviceName(final long deviceId) {
      String name = ALC10.alcGetString(deviceId, 4115);
      if (name == null) {
         name = ALC10.alcGetString(deviceId, 4101);
      }

      if (name == null) {
         name = "Unknown (0x" + HexFormat.of().toHexDigits(deviceId) + ")";
      }

      return name;
   }

   private static long openDeviceOrFallback(final @Nullable String preferredDevice, final @Nullable String systemDefaultDevice) {
      OptionalLong device = OptionalLong.empty();
      if (preferredDevice != null) {
         device = tryOpenDevice(preferredDevice);
      }

      if (device.isEmpty() && systemDefaultDevice != null) {
         device = tryOpenDevice(systemDefaultDevice);
      }

      if (device.isEmpty()) {
         device = tryOpenDevice(null);
      }

      if (device.isEmpty()) {
         throw new IllegalStateException("Failed to open OpenAL device");
      } else {
         return device.getAsLong();
      }
   }

   private static OptionalLong tryOpenDevice(final @Nullable String name) {
      long device = ALC10.alcOpenDevice(name);
      return device != 0L && !OpenAlUtil.checkALCError(device, "Open device") ? OptionalLong.of(device) : OptionalLong.empty();
   }

   public void cleanup() {
      this.staticChannels.cleanup();
      this.streamingChannels.cleanup();
      ALC10.alcDestroyContext(this.context);
      if (this.currentDevice != 0L) {
         ALC10.alcCloseDevice(this.currentDevice);
      }
   }

   public Listener getListener() {
      return this.listener;
   }

   public @Nullable Channel acquireChannel(final Library.Pool pool) {
      return (pool == Library.Pool.STREAMING ? this.streamingChannels : this.staticChannels).acquire();
   }

   public void releaseChannel(final Channel channel) {
      if (!this.staticChannels.release(channel) && !this.streamingChannels.release(channel)) {
         throw new IllegalStateException("Tried to release unknown channel");
      }
   }

   public String getChannelDebugString() {
      return String.format(
         Locale.ROOT,
         "Sounds: %d/%d + %d/%d",
         this.staticChannels.getUsedCount(),
         this.staticChannels.getMaxCount(),
         this.streamingChannels.getUsedCount(),
         this.streamingChannels.getMaxCount()
      );
   }

   public boolean isCurrentDeviceDisconnected() {
      return this.supportsDisconnections && ALC11.alcGetInteger(this.currentDevice, 787) == 0;
   }

   public static DeviceTracker createDeviceTracker() {
      DeviceList deviceList = DeviceList.query();
      if (CallbackDeviceTracker.isSupported()) {
         LOGGER.debug("Using SOFT_system_events callback for tracking audio device changes");
         return CallbackDeviceTracker.createAndInstall(deviceList);
      } else {
         LOGGER.debug("Using polling for tracking audio device changes");
         return new PollingDeviceTracker(deviceList);
      }
   }

   private interface ChannelPool {
      @Nullable Channel acquire();

      boolean release(Channel channel);

      void cleanup();

      int getMaxCount();

      int getUsedCount();
   }

   private static class CountingChannelPool implements Library.ChannelPool {
      private final int limit;
      private final Set<Channel> activeChannels = Sets.newIdentityHashSet();

      public CountingChannelPool(final int limit) {
         this.limit = limit;
      }

      @Override
      public @Nullable Channel acquire() {
         if (this.activeChannels.size() >= this.limit) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
               Library.LOGGER.warn("Maximum sound pool size {} reached", this.limit);
            }

            return null;
         } else {
            Channel channel = Channel.create();
            if (channel != null) {
               this.activeChannels.add(channel);
            }

            return channel;
         }
      }

      @Override
      public boolean release(final Channel channel) {
         if (!this.activeChannels.remove(channel)) {
            return false;
         }

         channel.destroy();
         return true;
      }

      @Override
      public void cleanup() {
         this.activeChannels.forEach(Channel::destroy);
         this.activeChannels.clear();
      }

      @Override
      public int getMaxCount() {
         return this.limit;
      }

      @Override
      public int getUsedCount() {
         return this.activeChannels.size();
      }
   }

   public enum Pool {
      STATIC,
      STREAMING;
   }
}
