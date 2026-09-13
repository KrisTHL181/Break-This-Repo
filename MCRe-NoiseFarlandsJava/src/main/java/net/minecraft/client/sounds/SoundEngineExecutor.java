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
        super("Sound executor", false);
    }

    private Thread createThread() {
        Thread thread = new Thread(this::run, "Sound engine");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(
            (t, e) -> Minecraft.getInstance().delayCrash(CrashReport.forThrowable(e, "Uncaught exception on thread: " + t.getName()))
        );
        thread.start();
        return thread;
    }

    @Override
    public Runnable wrapRunnable(final Runnable runnable) {
        return runnable;
    }

    @Override
    public void schedule(final Runnable runnable) {
        if (!this.shutdown) {
            super.schedule(runnable);
        }
    }

    @Override
    protected boolean shouldRun(final Runnable task) {
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
    protected void waitForTasks() {
        LockSupport.park("waiting for tasks");
    }

    public void shutDown() {
        this.shutdown = true;
        this.dropAllTasks();
        this.thread.interrupt();

        try {
            this.thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void startUp() {
        this.shutdown = false;
        this.thread = this.createThread();
    }
}