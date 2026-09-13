package com.mojang.jtracy;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class TracyClient {
   private static boolean loaded = false;
   private static AtomicInteger lastGpuContextId = new AtomicInteger(0);

   public static boolean isAvailable() {
      return loaded;
   }

   public static synchronized void load() throws UnsatisfiedLinkError {
      if (!loaded) {
         new Loader().load();
         loaded = true;
      }
   }

   public static void markFrame() {
      if (loaded) {
         TracyBindings.markFrame(0L);
      }
   }

   public static void frameImage(ByteBuffer image, int width, int height, int offset, boolean flip) {
      if (loaded) {
         TracyBindings.frameImage(image, width, height, offset, flip);
      }
   }

   public static Zone beginZone(String name, boolean captureSource) {
      if (loaded) {
         String function = "";
         String file = "";
         int line = 0;
         if (captureSource) {
            StackWalker walker = StackWalker.getInstance(Set.of(Option.RETAIN_CLASS_REFERENCE), 2);
            Optional<StackFrame> result = walker.walk(s -> s.filter(framex -> framex.getDeclaringClass() != TracyClient.class).findFirst());
            if (result.isPresent()) {
               StackFrame frame = result.get();
               function = frame.getMethodName();
               file = frame.getFileName();
               line = frame.getLineNumber();
            }
         }

         return new Zone(TracyBindings.beginZone(name, function, file, line));
      } else {
         return Zone.UNAVAILABLE;
      }
   }

   public static Zone beginZone(String name, String function, String file, int line) {
      return loaded ? new Zone(TracyBindings.beginZone(name, function, file, line)) : Zone.UNAVAILABLE;
   }

   public static void setThreadName(String name, int group) {
      if (loaded) {
         TracyBindings.setThreadName(name, group);
      }
   }

   public static Plot createPlot(String name) {
      return loaded ? new Plot(TracyBindings.leakName(name)) : Plot.UNAVAILABLE;
   }

   public static DiscontinuousFrame createDiscontinuousFrame(String name) {
      return loaded ? new DiscontinuousFrame(TracyBindings.leakName(name)) : DiscontinuousFrame.UNAVAILABLE;
   }

   public static ContinuousFrame createContinuousFrame(String name) {
      return loaded ? new ContinuousFrame(TracyBindings.leakName(name)) : ContinuousFrame.UNAVAILABLE;
   }

   public static MemoryPool createMemoryPool(String name) {
      return loaded ? new MemoryPool(TracyBindings.leakName(name)) : MemoryPool.UNAVAILABLE;
   }

   public static void reportAppInfo(String text) {
      if (loaded) {
         TracyBindings.appInfo(text);
      }
   }

   public static void message(String text) {
      if (loaded) {
         TracyBindings.message(text);
      }
   }

   public static void message(String text, int color) {
      if (loaded) {
         TracyBindings.messageColored(text, color);
      }
   }

   public static void message(Supplier<String> text) {
      if (loaded) {
         TracyBindings.message(text.get());
      }
   }

   public static void message(Supplier<String> text, int color) {
      if (loaded) {
         TracyBindings.messageColored(text.get(), color);
      }
   }

   public static GpuContext createGpuContext(GpuApi api, long gpuTimestamp, float gpuPeriod) {
      if (loaded) {
         int id = lastGpuContextId.incrementAndGet();
         if (id == 255) {
            throw new UnsupportedOperationException("Too many GPU contexts were created");
         }

         TracyBindings.newGpuContext(id, gpuTimestamp, gpuPeriod, 0, api.getId());
         return new GpuContext(id);
      } else {
         return GpuContext.UNAVAILABLE;
      }
   }
}
