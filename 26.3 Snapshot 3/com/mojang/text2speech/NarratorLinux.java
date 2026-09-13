package com.mojang.text2speech;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class NarratorLinux implements Narrator {
   private final AtomicInteger executionBatch = new AtomicInteger();
   private final Pointer voiceCmuUsKal16;
   private final ExecutorService executor;

   public NarratorLinux() throws Narrator.InitializeException {
      NarratorLinux.FliteLibrary.loadNative();
      NarratorLinux.FliteLibrary.CmuUsKal16.loadNative();
      int rc = NarratorLinux.FliteLibrary.flite_init();
      if (rc != 0) {
         throw new Narrator.InitializeException("flite returned code " + rc);
      }

      this.voiceCmuUsKal16 = NarratorLinux.FliteLibrary.CmuUsKal16.register_cmu_us_kal16(null);
      if (this.voiceCmuUsKal16 == Pointer.NULL) {
         throw new Narrator.InitializeException("flite_cmu_us_kal16 failed to register");
      }

      this.executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("Narrator #%d").setDaemon(true).build());
   }

   @Override
   public void say(String msg, boolean interrupt, float volume) {
      if (interrupt) {
         this.clear();
      }

      int thisBatch = this.executionBatch.get();
      Arrays.stream(msg.split("[,.:;/\"()\\[\\]{}!?\\\\]+")).filter(x -> !x.isBlank()).forEach(unit -> this.executor.submit(() -> {
         if (thisBatch >= this.executionBatch.get()) {
            Pointer utterance = NarratorLinux.FliteLibrary.flite_synth_text(unit, this.voiceCmuUsKal16);

            try {
               Pointer wave = NarratorLinux.FliteLibrary.utt_wave(utterance);
               if (volume != 1.0F) {
                  int volumeFactor = (int)(volume * 65536.0F);
                  NarratorLinux.FliteLibrary.cst_wave_rescale(wave, volumeFactor);
               }

               NarratorLinux.FliteLibrary.play_wave(wave);
            } finally {
               NarratorLinux.FliteLibrary.delete_utterance(utterance);
            }
         }
      }));
   }

   @Override
   public void clear() {
      this.executionBatch.incrementAndGet();
   }

   @Override
   public void destroy() {
      this.executor.shutdownNow();
   }

   private static class FliteLibrary {
      private static final int SUCCESS = 0;
      private static final String NATIVE_LIBRARY_NAME = "flite";

      public static void loadNative() throws Narrator.InitializeException {
         try {
            Native.register(NarratorLinux.FliteLibrary.class, NativeLibrary.getInstance("flite"));
         } catch (Throwable e) {
            throw new Narrator.InitializeException("Failed to load library flite", e);
         }
      }

      private static native int flite_init();

      private static native Pointer flite_synth_text(String var0, Pointer var1);

      private static native Pointer utt_wave(Pointer var0);

      private static native void play_wave(Pointer var0);

      private static native void cst_wave_rescale(Pointer var0, int var1);

      private static native void delete_utterance(Pointer var0);

      private static class CmuUsKal16 {
         private static final String NATIVE_LIBRARY_NAME = "flite_cmu_us_kal16";

         public static void loadNative() throws Narrator.InitializeException {
            try {
               Native.register(NarratorLinux.FliteLibrary.CmuUsKal16.class, NativeLibrary.getInstance("flite_cmu_us_kal16"));
            } catch (Throwable e) {
               throw new Narrator.InitializeException("Failed to load library flite_cmu_us_kal16", e);
            }
         }

         private static native Pointer register_cmu_us_kal16(String var0);
      }
   }
}
