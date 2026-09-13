package net.minecraft.util.profiling;

import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.MetricCategory;

public interface ProfilerFiller {
   String ROOT = "root";

   void startTick();

   void endTick();

   void push(String var1);

   void push(Supplier<String> var1);

   void pop();

   void popPush(String var1);

   void popPush(Supplier<String> var1);

   default void addZoneText(String p_363858_) {
   }

   default void addZoneValue(long p_366219_) {
   }

   default void setZoneColor(int p_364754_) {
   }

   default Zone zone(String p_361931_) {
      this.push(p_361931_);
      return new Zone(this);
   }

   default Zone zone(Supplier<String> p_366899_) {
      this.push(p_366899_);
      return new Zone(this);
   }

   void markForCharting(MetricCategory var1);

   default void incrementCounter(String p_18585_) {
      this.incrementCounter(p_18585_, 1);
   }

   void incrementCounter(String var1, int var2);

   default void incrementCounter(Supplier<String> p_18586_) {
      this.incrementCounter(p_18586_, 1);
   }

   void incrementCounter(Supplier<String> var1, int var2);

   static ProfilerFiller combine(ProfilerFiller p_369473_, ProfilerFiller p_362839_) {
      if (p_369473_ == InactiveProfiler.INSTANCE) {
         return p_362839_;
      } else {
         return p_362839_ == InactiveProfiler.INSTANCE ? p_369473_ : new ProfilerFiller.CombinedProfileFiller(p_369473_, p_362839_);
      }
   }

   class CombinedProfileFiller implements ProfilerFiller {
      private final ProfilerFiller first;
      private final ProfilerFiller second;

      public CombinedProfileFiller(ProfilerFiller p_361593_, ProfilerFiller p_363992_) {
         this.first = p_361593_;
         this.second = p_363992_;
      }

      @Override
      public void startTick() {
         this.first.startTick();
         this.second.startTick();
      }

      @Override
      public void endTick() {
         this.first.endTick();
         this.second.endTick();
      }

      @Override
      public void push(String p_363352_) {
         this.first.push(p_363352_);
         this.second.push(p_363352_);
      }

      @Override
      public void push(Supplier<String> p_361348_) {
         this.first.push(p_361348_);
         this.second.push(p_361348_);
      }

      @Override
      public void markForCharting(MetricCategory p_365312_) {
         this.first.markForCharting(p_365312_);
         this.second.markForCharting(p_365312_);
      }

      @Override
      public void pop() {
         this.first.pop();
         this.second.pop();
      }

      @Override
      public void popPush(String p_364738_) {
         this.first.popPush(p_364738_);
         this.second.popPush(p_364738_);
      }

      @Override
      public void popPush(Supplier<String> p_361184_) {
         this.first.popPush(p_361184_);
         this.second.popPush(p_361184_);
      }

      @Override
      public void incrementCounter(String p_368612_, int p_365761_) {
         this.first.incrementCounter(p_368612_, p_365761_);
         this.second.incrementCounter(p_368612_, p_365761_);
      }

      @Override
      public void incrementCounter(Supplier<String> p_365250_, int p_365517_) {
         this.first.incrementCounter(p_365250_, p_365517_);
         this.second.incrementCounter(p_365250_, p_365517_);
      }

      @Override
      public void addZoneText(String p_369699_) {
         this.first.addZoneText(p_369699_);
         this.second.addZoneText(p_369699_);
      }

      @Override
      public void addZoneValue(long p_362373_) {
         this.first.addZoneValue(p_362373_);
         this.second.addZoneValue(p_362373_);
      }

      @Override
      public void setZoneColor(int p_365533_) {
         this.first.setZoneColor(p_365533_);
         this.second.setZoneColor(p_365533_);
      }
   }
}
