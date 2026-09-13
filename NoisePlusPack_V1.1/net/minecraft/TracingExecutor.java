package net.minecraft;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public record TracingExecutor(ExecutorService service) implements Executor {
   public Executor forName(String p_364709_) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         return p_369604_ -> this.service.execute(() -> {
            Thread thread = Thread.currentThread();
            String s = thread.getName();
            thread.setName(p_364709_);

            try {
               Zone zone = TracyClient.beginZone(p_364709_, SharedConstants.IS_RUNNING_IN_IDE);

               try {
                  p_369604_.run();
               } catch (Throwable var12) {
                  if (zone != null) {
                     try {
                        zone.close();
                     } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                     }
                  }

                  throw var12;
               }

               if (zone != null) {
                  zone.close();
               }
            } finally {
               thread.setName(s);
            }
         });
      } else {
         return TracyClient.isAvailable() ? p_366279_ -> this.service.execute(() -> {
            Zone zone = TracyClient.beginZone(p_364709_, SharedConstants.IS_RUNNING_IN_IDE);

            try {
               p_366279_.run();
            } catch (Throwable var6) {
               if (zone != null) {
                  try {
                     zone.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (zone != null) {
               zone.close();
            }
         }) : this.service;
      }
   }

   @Override
   public void execute(Runnable p_362236_) {
      this.service.execute(wrapUnnamed(p_362236_));
   }

   public void shutdownAndAwait(long p_367055_, TimeUnit p_369186_) {
      this.service.shutdown();

      boolean flag;
      try {
         flag = this.service.awaitTermination(p_367055_, p_369186_);
      } catch (InterruptedException interruptedexception) {
         flag = false;
      }

      if (!flag) {
         this.service.shutdownNow();
      }
   }

   private static Runnable wrapUnnamed(Runnable p_362176_) {
      return !TracyClient.isAvailable() ? p_362176_ : () -> {
         Zone zone = TracyClient.beginZone("task", SharedConstants.IS_RUNNING_IN_IDE);

         try {
            p_362176_.run();
         } catch (Throwable var5) {
            if (zone != null) {
               try {
                  zone.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (zone != null) {
            zone.close();
         }
      };
   }
}
