package net.minecraft;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public record TracingExecutor(ExecutorService service) implements Executor {
   public Executor forName(final String name) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         return command -> this.service.execute(() -> {
            Thread thread = Thread.currentThread();
            String oldName = thread.getName();
            thread.setName(name);

            try {
               Zone ignored = TracyClient.beginZone(name, SharedConstants.IS_RUNNING_IN_IDE);

               try {
                  command.run();
               } catch (Throwable t$) {
                  if (ignored != null) {
                     try {
                        ignored.close();
                     } catch (Throwable x2) {
                        t$.addSuppressed(x2);
                     }
                  }

                  throw t$;
               }

               if (ignored != null) {
                  ignored.close();
               }
            } finally {
               thread.setName(oldName);
            }
         });
      } else {
         return TracyClient.isAvailable() ? command -> this.service.execute(() -> {
            Zone ignored = TracyClient.beginZone(name, SharedConstants.IS_RUNNING_IN_IDE);

            try {
               command.run();
            } catch (Throwable t$) {
               if (ignored != null) {
                  try {
                     ignored.close();
                  } catch (Throwable x2) {
                     t$.addSuppressed(x2);
                  }
               }

               throw t$;
            }

            if (ignored != null) {
               ignored.close();
            }
         }) : this.service;
      }
   }

   @Override
   public void execute(final Runnable command) {
      this.service.execute(wrapUnnamed(command));
   }

   public void shutdownAndAwait(final long timeout, final TimeUnit unit) {
      this.service.shutdown();

      boolean terminated;
      try {
         terminated = this.service.awaitTermination(timeout, unit);
      } catch (InterruptedException e) {
         terminated = false;
      }

      if (!terminated) {
         this.service.shutdownNow();
      }
   }

   private static Runnable wrapUnnamed(final Runnable command) {
      return !TracyClient.isAvailable() ? command : () -> {
         Zone ignored = TracyClient.beginZone("task", SharedConstants.IS_RUNNING_IN_IDE);

         try {
            command.run();
         } catch (Throwable t$) {
            if (ignored != null) {
               try {
                  ignored.close();
               } catch (Throwable x2) {
                  t$.addSuppressed(x2);
               }
            }

            throw t$;
         }

         if (ignored != null) {
            ignored.close();
         }
      };
   }
}
