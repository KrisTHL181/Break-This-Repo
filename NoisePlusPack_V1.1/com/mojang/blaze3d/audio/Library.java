package com.mojang.blaze3d.audio;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.openal.ALUtil;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Library {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_DEVICE = 0;
   private static final int DEFAULT_CHANNEL_COUNT = 30;
   private long currentDevice;
   private long context;
   private boolean supportsDisconnections;
   private @Nullable String defaultDeviceName;
   private static final Library.ChannelPool EMPTY = new Library.ChannelPool() {
      @Override
      public @Nullable Channel acquire() {
         return null;
      }

      @Override
      public boolean release(Channel p_83708_) {
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

   public Library() {
      this.defaultDeviceName = getDefaultDeviceName();
   }

   public void init(@Nullable String p_231085_, boolean p_231086_) {
      this.currentDevice = openDeviceOrFallback(p_231085_);
      this.supportsDisconnections = false;
      ALCCapabilities alccapabilities = ALC.createCapabilities(this.currentDevice);
      if (OpenAlUtil.checkALCError(this.currentDevice, "Get capabilities")) {
         throw new IllegalStateException("Failed to get OpenAL capabilities");
      }

      if (!alccapabilities.OpenALC11) {
         throw new IllegalStateException("OpenAL 1.1 not supported");
      }

      MemoryStack memorystack = MemoryStack.stackPush();

      try {
         IntBuffer intbuffer = this.createAttributes(memorystack, alccapabilities.ALC_SOFT_HRTF && p_231086_);
         this.context = ALC10.alcCreateContext(this.currentDevice, intbuffer);
      } catch (Throwable var9) {
         if (memorystack != null) {
            try {
               memorystack.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (memorystack != null) {
         memorystack.close();
      }

      if (OpenAlUtil.checkALCError(this.currentDevice, "Create context")) {
         throw new IllegalStateException("Unable to create OpenAL context");
      }

      ALC10.alcMakeContextCurrent(this.context);
      int j = this.getChannelCount();
      int k = Mth.clamp((int)Mth.sqrt(j), 2, 8);
      int i = Mth.clamp(j - k, 8, 255);
      this.staticChannels = new Library.CountingChannelPool(i);
      this.streamingChannels = new Library.CountingChannelPool(k);
      ALCapabilities alcapabilities = AL.createCapabilities(alccapabilities);
      OpenAlUtil.checkALError("Initialization");
      if (!alcapabilities.AL_EXT_source_distance_model) {
         throw new IllegalStateException("AL_EXT_source_distance_model is not supported");
      }

      AL10.alEnable(512);
      if (!alcapabilities.AL_EXT_LINEAR_DISTANCE) {
         throw new IllegalStateException("AL_EXT_LINEAR_DISTANCE is not supported");
      }

      OpenAlUtil.checkALError("Enable per-source distance models");
      LOGGER.info("OpenAL initialized on device {}", this.getCurrentDeviceName());
      this.supportsDisconnections = ALC10.alcIsExtensionPresent(this.currentDevice, "ALC_EXT_disconnect");
   }

   private IntBuffer createAttributes(MemoryStack p_396417_, boolean p_397006_) {
      int i = 5;
      IntBuffer intbuffer = p_396417_.callocInt(11);
      int j = ALC10.alcGetInteger(this.currentDevice, 6548);
      if (j > 0) {
         intbuffer.put(6546).put(p_397006_ ? 1 : 0);
         intbuffer.put(6550).put(0);
      }

      intbuffer.put(6554).put(1);
      return intbuffer.put(0).flip();
   }

   private int getChannelCount() {
      MemoryStack memorystack = MemoryStack.stackPush();

      int var7;
      label58: {
         try {
            int i = ALC10.alcGetInteger(this.currentDevice, 4098);
            if (OpenAlUtil.checkALCError(this.currentDevice, "Get attributes size")) {
               throw new IllegalStateException("Failed to get OpenAL attributes");
            }

            IntBuffer intbuffer = memorystack.mallocInt(i);
            ALC10.alcGetIntegerv(this.currentDevice, 4099, intbuffer);
            if (OpenAlUtil.checkALCError(this.currentDevice, "Get attributes")) {
               throw new IllegalStateException("Failed to get OpenAL attributes");
            }

            int j = 0;

            while (j < i) {
               int k = intbuffer.get(j++);
               if (k == 0) {
                  break;
               }

               int l = intbuffer.get(j++);
               if (k == 4112) {
                  var7 = l;
                  break label58;
               }
            }
         } catch (Throwable var9) {
            if (memorystack != null) {
               try {
                  memorystack.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (memorystack != null) {
            memorystack.close();
         }

         return 30;
      }

      if (memorystack != null) {
         memorystack.close();
      }

      return var7;
   }

   public static @Nullable String getDefaultDeviceName() {
      if (!ALC10.alcIsExtensionPresent(0L, "ALC_ENUMERATE_ALL_EXT")) {
         return null;
      }

      ALUtil.getStringList(0L, 4115);
      return ALC10.alcGetString(0L, 4114);
   }

   public String getCurrentDeviceName() {
      String s = ALC10.alcGetString(this.currentDevice, 4115);
      if (s == null) {
         s = ALC10.alcGetString(this.currentDevice, 4101);
      }

      if (s == null) {
         s = "Unknown";
      }

      return s;
   }

   public synchronized boolean hasDefaultDeviceChanged() {
      String s = getDefaultDeviceName();
      if (Objects.equals(this.defaultDeviceName, s)) {
         return false;
      }

      this.defaultDeviceName = s;
      return true;
   }

   private static long openDeviceOrFallback(@Nullable String p_193473_) {
      OptionalLong optionallong = OptionalLong.empty();
      if (p_193473_ != null) {
         optionallong = tryOpenDevice(p_193473_);
      }

      if (optionallong.isEmpty()) {
         optionallong = tryOpenDevice(getDefaultDeviceName());
      }

      if (optionallong.isEmpty()) {
         optionallong = tryOpenDevice(null);
      }

      if (optionallong.isEmpty()) {
         throw new IllegalStateException("Failed to open OpenAL device");
      } else {
         return optionallong.getAsLong();
      }
   }

   private static OptionalLong tryOpenDevice(@Nullable String p_193476_) {
      long i = ALC10.alcOpenDevice(p_193476_);
      return i != 0L && !OpenAlUtil.checkALCError(i, "Open device") ? OptionalLong.of(i) : OptionalLong.empty();
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

   public @Nullable Channel acquireChannel(Library.Pool p_83698_) {
      return (p_83698_ == Library.Pool.STREAMING ? this.streamingChannels : this.staticChannels).acquire();
   }

   public void releaseChannel(Channel p_83696_) {
      if (!this.staticChannels.release(p_83696_) && !this.streamingChannels.release(p_83696_)) {
         throw new IllegalStateException("Tried to release unknown channel");
      }
   }

   public String getDebugString() {
      return String.format(
         Locale.ROOT,
         "Sounds: %d/%d + %d/%d",
         this.staticChannels.getUsedCount(),
         this.staticChannels.getMaxCount(),
         this.streamingChannels.getUsedCount(),
         this.streamingChannels.getMaxCount()
      );
   }

   public List<String> getAvailableSoundDevices() {
      List<String> list = ALUtil.getStringList(0L, 4115);
      return list == null ? Collections.emptyList() : list;
   }

   public boolean isCurrentDeviceDisconnected() {
      return this.supportsDisconnections && ALC11.alcGetInteger(this.currentDevice, 787) == 0;
   }

   @OnlyIn(Dist.CLIENT)
   interface ChannelPool {
      @Nullable Channel acquire();

      boolean release(Channel var1);

      void cleanup();

      int getMaxCount();

      int getUsedCount();
   }

   @OnlyIn(Dist.CLIENT)
   static class CountingChannelPool implements Library.ChannelPool {
      private final int limit;
      private final Set<Channel> activeChannels = Sets.newIdentityHashSet();

      public CountingChannelPool(int p_83716_) {
         this.limit = p_83716_;
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
      public boolean release(Channel p_83719_) {
         if (!this.activeChannels.remove(p_83719_)) {
            return false;
         }

         p_83719_.destroy();
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

   @OnlyIn(Dist.CLIENT)
   public enum Pool {
      STATIC,
      STREAMING;
   }
}
