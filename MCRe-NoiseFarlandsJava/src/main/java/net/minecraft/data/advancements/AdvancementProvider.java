package net.minecraft.data.advancements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

public class AdvancementProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;
    private final List<AdvancementSubProvider> subProviders;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public AdvancementProvider(
        final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries, final List<AdvancementSubProvider> subProviders
    ) {
        this.pathProvider = output.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
        this.subProviders = subProviders;
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        return this.registries.thenCompose(lookup -> {
            Set<Identifier> allAdvancements = new HashSet<>();
            List<CompletableFuture<?>> tasks = new ArrayList<>();
            Consumer<AdvancementHolder> consumer = holder -> {
                if (!allAdvancements.add(holder.id())) {
                    throw new IllegalStateException("Duplicate advancement " + holder.id());
                }

                Path path = this.pathProvider.json(holder.id());
                tasks.add(DataProvider.saveStable(cache, lookup, Advancement.CODEC, holder.value(), path));
            };

            for (AdvancementSubProvider subProvider : this.subProviders) {
                subProvider.generate(lookup, consumer);
            }

            return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public final String getName() {
        return "Advancements";
    }
}