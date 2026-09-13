package net.minecraft.client.gui.narration;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScreenNarrationCollector {
    private int generation;
    private final Map<ScreenNarrationCollector.EntryKey, ScreenNarrationCollector.NarrationEntry> entries = Maps.newTreeMap(
        Comparator.<ScreenNarrationCollector.EntryKey, NarratedElementType>comparing(e -> e.type).thenComparing(e -> e.depth)
    );

    public void update(final Consumer<NarrationElementOutput> updater) {
        this.generation++;
        updater.accept(new ScreenNarrationCollector.Output(0));
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

    @OnlyIn(Dist.CLIENT)
    private record EntryKey(NarratedElementType type, int depth) {
    }

    @OnlyIn(Dist.CLIENT)
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

    @OnlyIn(Dist.CLIENT)
    private class Output implements NarrationElementOutput {
        private final int depth;

        private Output(final int depth) {
            this.depth = depth;
        }

        @Override
        public void add(final NarratedElementType type, final NarrationThunk<?> contents) {
            ScreenNarrationCollector.this.entries
                .computeIfAbsent(new ScreenNarrationCollector.EntryKey(type, this.depth), k -> new ScreenNarrationCollector.NarrationEntry())
                .update(ScreenNarrationCollector.this.generation, contents);
        }

        @Override
        public NarrationElementOutput nest() {
            return ScreenNarrationCollector.this.new Output(this.depth + 1);
        }
    }
}