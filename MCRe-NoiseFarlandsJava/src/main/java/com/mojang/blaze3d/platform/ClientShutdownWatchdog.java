package com.mojang.blaze3d.platform;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.server.dedicated.ServerWatchdog;
import net.minecraft.util.NativeModuleLister;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientShutdownWatchdog {
    private static final Duration CRASH_REPORT_PRELOAD_LOAD = Duration.ofSeconds(15L);
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static final int SHUTDOWN_STARTED_ID = Integer.MIN_VALUE;

    public static void startShutdownWatchdog(
        final String callsite, final boolean forceShutdown, final @Nullable Minecraft minecraft, final GameConfig gameConfig, final long mainThreadId
    ) {
        int id = THREAD_COUNTER.incrementAndGet();
        if (id >= 0) {
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(CRASH_REPORT_PRELOAD_LOAD);
                } catch (InterruptedException e) {
                    return;
                }

                if (THREAD_COUNTER.compareAndSet(id, Integer.MIN_VALUE)) {
                    CrashReport report = ServerWatchdog.createWatchdogCrashReport("Client shutdown from " + callsite, mainThreadId);
                    CrashReportCategory details = report.addCategory("Client watchdog shutdown details");
                    NativeModuleLister.addCrashSection(details);
                    if (minecraft != null) {
                        minecraft.fillReport(report);
                    } else {
                        Minecraft.fillReport(null, null, gameConfig.game.launchVersion, null, report);
                    }

                    Minecraft.saveReport(gameConfig.location.gameDirectory, report);
                    if (forceShutdown) {
                        System.exit(-8);
                    }
                }
            }, "Client shutdown watchdog #" + id);
            thread.setDaemon(true);
            thread.start();
        }
    }
}