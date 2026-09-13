package net.minecraft.util.profiling;

import com.mojang.jtracy.TracyClient;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

public final class Profiler {
   private static final ThreadLocal<TracyZoneFiller> TRACY_FILLER = ThreadLocal.withInitial(TracyZoneFiller::new);
   private static final ThreadLocal<@Nullable ProfilerFiller> ACTIVE = new ThreadLocal<>();
   private static final AtomicInteger ACTIVE_COUNT = new AtomicInteger();

   private Profiler() {
   }

   public static Profiler.Scope use(final ProfilerFiller filler) {
      if (filler instanceof InactiveProfiler) {
         return () -> {};
      }

      startUsing(filler);
      return Profiler::stopUsing;
   }

   private static void startUsing(final ProfilerFiller filler) {
      if (ACTIVE.get() != null) {
         throw new IllegalStateException("Profiler is already active");
      }

      ProfilerFiller active = decorateFiller(filler);
      ACTIVE.set(active);
      ACTIVE_COUNT.incrementAndGet();
      active.startTick();
   }

   private static void stopUsing() {
      ProfilerFiller active = ACTIVE.get();
      if (active == null) {
         throw new IllegalStateException("Profiler was not active");
      }

      ACTIVE.remove();
      ACTIVE_COUNT.decrementAndGet();
      active.endTick();
   }

   private static ProfilerFiller decorateFiller(final ProfilerFiller filler) {
      return ProfilerFiller.combine(getDefaultFiller(), filler);
   }

   public static ProfilerFiller get() {
      return ACTIVE_COUNT.get() == 0 ? getDefaultFiller() : Objects.requireNonNullElseGet(ACTIVE.get(), Profiler::getDefaultFiller);
   }

   private static ProfilerFiller getDefaultFiller() {
      return TracyClient.isAvailable() ? TRACY_FILLER.get() : InactiveProfiler.INSTANCE;
   }

   public interface Scope extends AutoCloseable {
      @Override
      void close();
   }
}
