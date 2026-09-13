package net.minecraft.util.profiling;

import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.slf4j.Logger;

public class TracyZoneFiller implements ProfilerFiller {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(Option.RETAIN_CLASS_REFERENCE), 5);
   private final List<com.mojang.jtracy.Zone> activeZones = new ArrayList<>();
   private final Map<String, TracyZoneFiller.PlotAndValue> plots = new HashMap<>();
   private final String name = Thread.currentThread().getName();

   @Override
   public void startTick() {
   }

   @Override
   public void endTick() {
      for (TracyZoneFiller.PlotAndValue tracyzonefiller$plotandvalue : this.plots.values()) {
         tracyzonefiller$plotandvalue.set(0);
      }
   }

   @Override
   public void push(String p_364548_) {
      String s = "";
      String s1 = "";
      int i = 0;
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         Optional<StackFrame> optional = STACK_WALKER.walk(
            p_361443_ -> p_361443_.filter(
                  p_366989_ -> p_366989_.getDeclaringClass() != TracyZoneFiller.class
                     && p_366989_.getDeclaringClass() != ProfilerFiller.CombinedProfileFiller.class
               )
               .findFirst()
         );
         if (optional.isPresent()) {
            StackFrame stackframe = optional.get();
            s = stackframe.getMethodName();
            s1 = stackframe.getFileName();
            i = stackframe.getLineNumber();
         }
      }

      com.mojang.jtracy.Zone zone = TracyClient.beginZone(p_364548_, s, s1, i);
      this.activeZones.add(zone);
   }

   @Override
   public void push(Supplier<String> p_367014_) {
      this.push(p_367014_.get());
   }

   @Override
   public void pop() {
      if (this.activeZones.isEmpty()) {
         LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
      } else {
         com.mojang.jtracy.Zone zone = this.activeZones.removeLast();
         zone.close();
      }
   }

   @Override
   public void popPush(String p_362480_) {
      this.pop();
      this.push(p_362480_);
   }

   @Override
   public void popPush(Supplier<String> p_368969_) {
      this.pop();
      this.push(p_368969_.get());
   }

   @Override
   public void markForCharting(MetricCategory p_360953_) {
   }

   @Override
   public void incrementCounter(String p_362137_, int p_362577_) {
      this.plots.computeIfAbsent(p_362137_, p_367016_ -> new TracyZoneFiller.PlotAndValue(this.name + " " + p_362137_)).add(p_362577_);
   }

   @Override
   public void incrementCounter(Supplier<String> p_362628_, int p_368047_) {
      this.incrementCounter(p_362628_.get(), p_368047_);
   }

   private com.mojang.jtracy.Zone activeZone() {
      return this.activeZones.getLast();
   }

   @Override
   public void addZoneText(String p_362912_) {
      this.activeZone().addText(p_362912_);
   }

   @Override
   public void addZoneValue(long p_366154_) {
      this.activeZone().addValue(p_366154_);
   }

   @Override
   public void setZoneColor(int p_363144_) {
      this.activeZone().setColor(p_363144_);
   }

   static final class PlotAndValue {
      private final Plot plot;
      private int value;

      PlotAndValue(String p_366532_) {
         this.plot = TracyClient.createPlot(p_366532_);
         this.value = 0;
      }

      void set(int p_362550_) {
         this.value = p_362550_;
         this.plot.setValue(p_362550_);
      }

      void add(int p_365380_) {
         this.set(this.value + p_365380_);
      }
   }
}
