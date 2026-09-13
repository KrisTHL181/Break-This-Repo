package net.minecraft.client.gui.components.debug;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryMemory implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("memory");
   private final DebugEntryMemory.AllocationRateCalculator allocationRateCalculator = new DebugEntryMemory.AllocationRateCalculator();

   @Override
   public void display(DebugScreenDisplayer p_427624_, @Nullable Level p_427960_, @Nullable LevelChunk p_428417_, @Nullable LevelChunk p_424370_) {
      long i = Runtime.getRuntime().maxMemory();
      long j = Runtime.getRuntime().totalMemory();
      long k = Runtime.getRuntime().freeMemory();
      long l = j - k;
      p_427624_.addToGroup(
         GROUP,
         List.of(
            String.format(Locale.ROOT, "Mem: %2d%% %03d/%03dMB", l * 100L / i, bytesToMegabytes(l), bytesToMegabytes(i)),
            String.format(Locale.ROOT, "Allocation rate: %03dMB/s", bytesToMegabytes(this.allocationRateCalculator.bytesAllocatedPerSecond(l))),
            String.format(Locale.ROOT, "Allocated: %2d%% %03dMB", j * 100L / i, bytesToMegabytes(j))
         )
      );
   }

   private static long bytesToMegabytes(long p_430348_) {
      return p_430348_ / 1024L / 1024L;
   }

   @Override
   public boolean isAllowed(boolean p_423576_) {
      return true;
   }

   @OnlyIn(Dist.CLIENT)
   static class AllocationRateCalculator {
      private static final int UPDATE_INTERVAL_MS = 500;
      private static final List<GarbageCollectorMXBean> GC_MBEANS = ManagementFactory.getGarbageCollectorMXBeans();
      private long lastTime = 0L;
      private long lastHeapUsage = -1L;
      private long lastGcCounts = -1L;
      private long lastRate = 0L;

      long bytesAllocatedPerSecond(long p_425558_) {
         long i = System.currentTimeMillis();
         if (i - this.lastTime < 500L) {
            return this.lastRate;
         }

         long j = gcCounts();
         if (this.lastTime != 0L && j == this.lastGcCounts) {
            double d0 = (double)TimeUnit.SECONDS.toMillis(1L) / (i - this.lastTime);
            long k = p_425558_ - this.lastHeapUsage;
            this.lastRate = Math.round(k * d0);
         }

         this.lastTime = i;
         this.lastHeapUsage = p_425558_;
         this.lastGcCounts = j;
         return this.lastRate;
      }

      private static long gcCounts() {
         long i = 0L;

         for (GarbageCollectorMXBean garbagecollectormxbean : GC_MBEANS) {
            i += garbagecollectormxbean.getCollectionCount();
         }

         return i;
      }
   }
}
