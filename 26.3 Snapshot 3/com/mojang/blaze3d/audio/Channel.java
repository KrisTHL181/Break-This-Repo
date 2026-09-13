package com.mojang.blaze3d.audio;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;

public class Channel {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int QUEUED_BUFFER_COUNT = 4;
   public static final int BUFFER_DURATION_SECONDS = 1;
   private final int source;
   private final AtomicBoolean initialized = new AtomicBoolean(true);
   private int streamingBufferSize = 16384;
   private @Nullable AudioStream stream;

   static @Nullable Channel create() {
      int[] newId = new int[1];
      AL10.alGenSources(newId);
      return OpenAlUtil.checkALError("Allocate new source") ? null : new Channel(newId[0]);
   }

   private Channel(final int src) {
      this.source = src;
   }

   public void destroy() {
      if (this.initialized.compareAndSet(true, false)) {
         AL10.alSourceStop(this.source);
         OpenAlUtil.checkALError("Stop");
         if (this.stream != null) {
            try {
               this.stream.close();
            } catch (IOException e) {
               LOGGER.error("Failed to close audio stream", e);
            }

            this.removeProcessedBuffers();
            this.stream = null;
         }

         AL10.alDeleteSources(new int[]{this.source});
         OpenAlUtil.checkALError("Cleanup");
      }
   }

   public void play() {
      AL10.alSourcePlay(this.source);
   }

   private int getState() {
      return !this.initialized.get() ? 4116 : AL10.alGetSourcei(this.source, 4112);
   }

   public void pause() {
      if (this.getState() == 4114) {
         AL10.alSourcePause(this.source);
      }
   }

   public void unpause() {
      if (this.getState() == 4115) {
         AL10.alSourcePlay(this.source);
      }
   }

   public void stop() {
      if (this.initialized.get()) {
         AL10.alSourceStop(this.source);
         OpenAlUtil.checkALError("Stop");
      }
   }

   public boolean playing() {
      return this.getState() == 4114;
   }

   public boolean stopped() {
      return this.getState() == 4116;
   }

   public void setSelfPosition(final Vec3 newPosition) {
      AL10.alSourcefv(this.source, 4100, new float[]{(float)newPosition.x, (float)newPosition.y, (float)newPosition.z});
   }

   public void setPitch(final float pitch) {
      AL10.alSourcef(this.source, 4099, pitch);
   }

   public void setLooping(final boolean looping) {
      AL10.alSourcei(this.source, 4103, looping ? 1 : 0);
   }

   public void setVolume(final float volume) {
      AL10.alSourcef(this.source, 4106, volume);
   }

   public void disableAttenuation() {
      AL10.alSourcei(this.source, 53248, 0);
   }

   public void linearAttenuation(final float maxDistance) {
      AL10.alSourcei(this.source, 53248, 53251);
      AL10.alSourcef(this.source, 4131, maxDistance);
      AL10.alSourcef(this.source, 4129, 1.0F);
      AL10.alSourcef(this.source, 4128, 0.0F);
   }

   public void setRelative(final boolean relative) {
      AL10.alSourcei(this.source, 514, relative ? 1 : 0);
   }

   public void attachStaticBuffer(final SoundBuffer buffer) {
      buffer.getAlBuffer().ifPresent(bufferId -> AL10.alSourcei(this.source, 4105, bufferId));
   }

   public void attachBufferStream(final AudioStream stream) {
      this.stream = stream;
      AudioFormat format = stream.getFormat();
      this.streamingBufferSize = calculateBufferSize(format, 1);
      this.pumpBuffers(4);
   }

   private static int calculateBufferSize(final AudioFormat format, final int seconds) {
      return (int)(seconds * format.getSampleSizeInBits() / 8.0F * format.getChannels() * format.getSampleRate());
   }

   private void pumpBuffers(final int size) {
      if (this.stream != null) {
         try {
            for (int i = 0; i < size; i++) {
               ByteBuffer buffer = this.stream.read(this.streamingBufferSize);
               if (buffer != null) {
                  new SoundBuffer(buffer, this.stream.getFormat())
                     .releaseAlBuffer()
                     .ifPresent(bufferId -> AL10.alSourceQueueBuffers(this.source, new int[]{bufferId}));
               }
            }
         } catch (IOException e) {
            LOGGER.error("Failed to read from audio stream", e);
         }
      }
   }

   public void updateStream() {
      if (this.stream != null) {
         int processedBuffers = this.removeProcessedBuffers();
         this.pumpBuffers(processedBuffers);
      }
   }

   private int removeProcessedBuffers() {
      int processed = AL10.alGetSourcei(this.source, 4118);
      if (processed > 0) {
         int[] ids = new int[processed];
         AL10.alSourceUnqueueBuffers(this.source, ids);
         OpenAlUtil.checkALError("Unqueue buffers");
         AL10.alDeleteBuffers(ids);
         OpenAlUtil.checkALError("Remove processed buffers");
      }

      return processed;
   }
}
