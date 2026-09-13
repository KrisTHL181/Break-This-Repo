package net.minecraft.client.sounds;

import java.util.concurrent.locks.LockSupport;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoundEngineExecutor extends BlockableEventLoop<Runnable> {
   private Thread thread = this.createThread();
   private volatile boolean shutdown;

   public SoundEngineExecutor() {
      super("Sound executor");
   }

   private Thread createThread() {
      Thread thread = new Thread(this::run);
      thread.setDaemon(true);
      thread.setName("Sound engine");
      thread.setUncaughtExceptionHandler(
         (p_421083_, p_421084_) -> Minecraft.getInstance()
            .delayCrash(CrashReport.forThrowable(p_421084_, "Uncaught exception on thread: " + p_421083_.getName()))
      );
      thread.start();
      return thread;
   }

   @Override
   public Runnable wrapRunnable(Runnable p_120341_) {
      return p_120341_;
   }

   @Override
   public void schedule(Runnable p_429326_) {
      if (!this.shutdown) {
         super.schedule(p_429326_);
      }
   }

   @Override
   protected boolean shouldRun(Runnable p_120339_) {
      return !this.shutdown;
   }

   @Override
   protected Thread getRunningThread() {
      return this.thread;
   }

   private void run() {
      while (!this.shutdown) {
         this.managedBlock(() -> this.shutdown);
      }
   }

   @Override
   public void waitForTasks() {
      LockSupport.park("waiting for tasks");
   }

   public void shutDown() {
      this.shutdown = true;
      this.dropAllTasks();
      this.thread.interrupt();

      try {
         this.thread.join();
      } catch (InterruptedException interruptedexception) {
         Thread.currentThread().interrupt();
      }
   }

   public void startUp() {
      this.shutdown = false;
      this.thread = this.createThread();
   }
}
