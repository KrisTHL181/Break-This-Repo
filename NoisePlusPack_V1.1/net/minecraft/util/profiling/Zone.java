package net.minecraft.util.profiling;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class Zone implements AutoCloseable {
   public static final Zone INACTIVE = new Zone(null);
   private final @Nullable ProfilerFiller profiler;

   Zone(@Nullable ProfilerFiller p_363013_) {
      this.profiler = p_363013_;
   }

   public Zone addText(String p_367379_) {
      if (this.profiler != null) {
         this.profiler.addZoneText(p_367379_);
      }

      return this;
   }

   public Zone addText(Supplier<String> p_364057_) {
      if (this.profiler != null) {
         this.profiler.addZoneText(p_364057_.get());
      }

      return this;
   }

   public Zone addValue(long p_368374_) {
      if (this.profiler != null) {
         this.profiler.addZoneValue(p_368374_);
      }

      return this;
   }

   public Zone setColor(int p_361254_) {
      if (this.profiler != null) {
         this.profiler.setZoneColor(p_361254_);
      }

      return this;
   }

   @Override
   public void close() {
      if (this.profiler != null) {
         this.profiler.pop();
      }
   }
}
