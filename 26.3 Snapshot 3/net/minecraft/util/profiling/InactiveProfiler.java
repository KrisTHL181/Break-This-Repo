package net.minecraft.util.profiling;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.jspecify.annotations.Nullable;

public class InactiveProfiler implements ProfileCollector {
   public static final InactiveProfiler INSTANCE = new InactiveProfiler();

   private InactiveProfiler() {
   }

   @Override
   public void startTick() {
   }

   @Override
   public void endTick() {
   }

   @Override
   public void push(final String name) {
   }

   @Override
   public void push(final Supplier<String> name) {
   }

   @Override
   public void markForCharting(final MetricCategory category) {
   }

   @Override
   public void pop() {
   }

   @Override
   public void popPush(final String name) {
   }

   @Override
   public void popPush(final Supplier<String> name) {
   }

   @Override
   public Zone zone(final String name) {
      return Zone.INACTIVE;
   }

   @Override
   public Zone zone(final Supplier<String> name) {
      return Zone.INACTIVE;
   }

   @Override
   public void incrementCounter(final String name, final int amount) {
   }

   @Override
   public void incrementCounter(final Supplier<String> name, final int amount) {
   }

   @Override
   public ProfileResults getResults() {
      return EmptyProfileResults.EMPTY;
   }

   @Override
   public ActiveProfiler.@Nullable PathEntry getEntry(final String path) {
      return null;
   }

   @Override
   public Set<Pair<String, MetricCategory>> getChartedPaths() {
      return ImmutableSet.of();
   }
}
