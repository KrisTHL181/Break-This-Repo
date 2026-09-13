package net.minecraft.client.gui.narration;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;

public class ScreenNarrationCollector {
   private int generation;
   private final Map<ScreenNarrationCollector.EntryKey, ScreenNarrationCollector.NarrationEntry> entries = Maps.newTreeMap(
      Comparator.<ScreenNarrationCollector.EntryKey, NarratedElementType>comparing(e -> e.type).thenComparing(e -> e.depth)
   );

   public void update(final Consumer<NarrationElementOutput> updater, final NarrationTrigger narrationTrigger) {
      this.generation++;
      updater.accept(new ScreenNarrationCollector.Output(0, narrationTrigger));
   }

   public String collectNarrationText(final boolean force) {
      final StringBuilder result = new StringBuilder();
      Consumer<String> appender = new Consumer<String>() {
         private boolean firstEntry = true;

         public void accept(final String s) {
            if (!this.firstEntry) {
               result.append(". ");
            }

            this.firstEntry = false;
            result.append(s);
         }
      };
      this.entries.forEach((k, v) -> {
         if (v.generation == this.generation && (force || !v.alreadyNarrated)) {
            v.contents.getText(appender);
            v.alreadyNarrated = true;
         }
      });
      return result.toString();
   }

   private record EntryKey(NarratedElementType type, int depth) {
   }

   private static class NarrationEntry {
      private NarrationThunk<?> contents = NarrationThunk.EMPTY;
      private int generation = -1;
      private boolean alreadyNarrated;

      public ScreenNarrationCollector.NarrationEntry update(final int generation, final NarrationThunk<?> contents) {
         if (!this.contents.equals(contents)) {
            this.contents = contents;
            this.alreadyNarrated = false;
         } else if (this.generation + 1 != generation) {
            this.alreadyNarrated = false;
         }

         this.generation = generation;
         return this;
      }
   }

   private class Output implements NarrationElementOutput {
      private final int depth;
      private final NarrationTrigger narrationTrigger;

      private Output(final int depth, final NarrationTrigger narrationTrigger) {
         this.depth = depth;
         this.narrationTrigger = narrationTrigger;
      }

      @Override
      public void add(final NarratedElementType type, final NarrationThunk<?> contents) {
         ScreenNarrationCollector.this.entries
            .computeIfAbsent(new ScreenNarrationCollector.EntryKey(type, this.depth), k -> new ScreenNarrationCollector.NarrationEntry())
            .update(ScreenNarrationCollector.this.generation, contents);
      }

      @Override
      public NarrationElementOutput nest() {
         return ScreenNarrationCollector.this.new Output(this.depth + 1, this.narrationTrigger);
      }

      @Override
      public NarrationTrigger narrationTrigger() {
         return this.narrationTrigger;
      }
   }
}
