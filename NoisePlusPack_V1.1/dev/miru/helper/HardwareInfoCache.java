package dev.miru.helper;

import com.sun.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

public class HardwareInfoCache {
   private final ScheduledExecutorService scheduler;
   private final long refreshIntervalSeconds;
   private volatile HardwareInfoCache.HardwareSnapshot snapshot;

   public HardwareInfoCache(long refreshIntervalSeconds) {
      this.refreshIntervalSeconds = refreshIntervalSeconds;
      this.scheduler = new ScheduledThreadPoolExecutor(1);
      this.snapshot = new HardwareInfoCache.HardwareSnapshot();
      this.refresh();
      this.scheduler.scheduleAtFixedRate(this::safeRefresh, refreshIntervalSeconds, refreshIntervalSeconds, TimeUnit.SECONDS);
   }

   public HardwareInfoCache() {
      this(5L);
   }

   private void safeRefresh() {
      try {
         this.refresh();
      } catch (Throwable var2) {
      }
   }

   public synchronized void refresh() {
      OperatingSystemMXBean osBean = getOsBean();
      double cpuLoad = -1.0;
      long totalPhysical = -1L;
      long freePhysical = -1L;
      if (osBean != null) {
         try {
            cpuLoad = osBean.getCpuLoad();
         } catch (Throwable var11) {
         }

         try {
            totalPhysical = osBean.getTotalMemorySize();
            freePhysical = osBean.getFreeMemorySize();
         } catch (Throwable var10) {
         }
      }

      Map<String, HardwareInfoCache.DiskInfo> disks = collectDiskInfo();
      List<HardwareInfoCache.GpuInfo> gpus = collectGpuInfo();
      this.snapshot = new HardwareInfoCache.HardwareSnapshot(System.currentTimeMillis(), cpuLoad, totalPhysical, freePhysical, disks, gpus);
   }

   public HardwareInfoCache.HardwareSnapshot getSnapshot() {
      return this.snapshot;
   }

   public void stop() {
      this.scheduler.shutdownNow();
   }

   private static @Nullable OperatingSystemMXBean getOsBean() {
      try {
         java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
         if (bean instanceof OperatingSystemMXBean) {
            return (OperatingSystemMXBean)bean;
         }
      } catch (Throwable var1) {
      }

      return null;
   }

   private static Map<String, HardwareInfoCache.DiskInfo> collectDiskInfo() {
      try {
         Map<String, HardwareInfoCache.DiskInfo> map = new HashMap<>();

         for (File root : File.listRoots()) {
            try {
               String name = root.getAbsolutePath();
               long total = root.getTotalSpace();
               long free = root.getFreeSpace();
               map.put(name, new HardwareInfoCache.DiskInfo(total, free));
            } catch (Throwable var11) {
            }
         }

         try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
               try {
                  String name = store.toString();
                  long total = store.getTotalSpace();
                  long free = store.getUnallocatedSpace();
                  map.putIfAbsent(name, new HardwareInfoCache.DiskInfo(total, free));
               } catch (Throwable var10) {
               }
            }
         } catch (Throwable var12) {
         }

         return map;
      } catch (Throwable t) {
         return Collections.emptyMap();
      }
   }

   private static List<HardwareInfoCache.GpuInfo> collectGpuInfo() {
      List<HardwareInfoCache.GpuInfo> list = new ArrayList<>();

      try {
         ProcessBuilder pb = new ProcessBuilder("nvidia-smi", "--query-gpu=name,memory.total", "--format=csv,noheader");
         pb.redirectErrorStream(true);
         Process p = pb.start();

         String line;
         try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while ((line = r.readLine()) != null) {
               line = line.trim();
               if (!line.isEmpty()) {
                  String[] parts = line.split(",");
                  if (parts.length >= 1) {
                     String name = parts[0].trim();
                     long mem = -1L;
                     if (parts.length >= 2) {
                        String memStr = parts[1].replaceAll("[^0-9]", "");
                        if (!memStr.isEmpty()) {
                           try {
                              mem = Long.parseLong(memStr) * 1024L * 1024L;
                           } catch (NumberFormatException var12) {
                           }
                        }
                     }

                     list.add(new HardwareInfoCache.GpuInfo(name, mem));
                  }
               }
            }
         }

         p.destroyForcibly();
      } catch (Throwable var14) {
      }

      return Collections.unmodifiableList(list);
   }

   public record DiskInfo(long totalBytes, long freeBytes) {
   }

   public record GpuInfo(String name, long memoryBytes) {
   }

   public static final class HardwareSnapshot {
      public final long timestampMs;
      public final double systemCpuLoad;
      public final long totalPhysicalMemoryBytes;
      public final long freePhysicalMemoryBytes;
      public final Map<String, HardwareInfoCache.DiskInfo> diskInfo;
      public final List<HardwareInfoCache.GpuInfo> gpuInfo;

      private HardwareSnapshot() {
         this(System.currentTimeMillis(), -1.0, -1L, -1L, Collections.emptyMap(), Collections.emptyList());
      }

      public HardwareSnapshot(
         long timestampMs,
         double systemCpuLoad,
         long totalPhysicalMemoryBytes,
         long freePhysicalMemoryBytes,
         Map<String, HardwareInfoCache.DiskInfo> diskInfo,
         List<HardwareInfoCache.GpuInfo> gpuInfo
      ) {
         this.timestampMs = timestampMs;
         this.systemCpuLoad = systemCpuLoad;
         this.totalPhysicalMemoryBytes = totalPhysicalMemoryBytes;
         this.freePhysicalMemoryBytes = freePhysicalMemoryBytes;
         this.diskInfo = diskInfo == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(diskInfo));
         this.gpuInfo = gpuInfo == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(gpuInfo));
      }
   }
}
