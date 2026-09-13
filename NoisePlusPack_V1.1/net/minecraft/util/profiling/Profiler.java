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

   public static Profiler.Scope use(ProfilerFiller p_361779_) {
      startUsing(p_361779_);
      return Profiler::stopUsing;
   }

   private static void startUsing(ProfilerFiller p_367831_) {
      if (ACTIVE.get() != null) {
         throw new IllegalStateException("Profiler is already active");
      }

      ProfilerFiller profilerfiller = decorateFiller(p_367831_);
      ACTIVE.set(profilerfiller);
      ACTIVE_COUNT.incrementAndGet();
      profilerfiller.startTick();
   }

   private static void stopUsing() {
      ProfilerFiller profilerfiller = ACTIVE.get();
      if (profilerfiller == null) {
         throw new IllegalStateException("Profiler was not active");
      }

      ACTIVE.remove();
      ACTIVE_COUNT.decrementAndGet();
      profilerfiller.endTick();
   }

   private static ProfilerFiller decorateFiller(ProfilerFiller p_364301_) {
      return ProfilerFiller.combine(getDefaultFiller(), p_364301_);
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
