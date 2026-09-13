package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;

public abstract class TagsProvider<T> implements DataProvider {
   protected final PackOutput.PathProvider pathProvider;
   private final CompletableFuture<HolderLookup.Provider> lookupProvider;
   private final CompletableFuture<Void> contentsDone = new CompletableFuture<>();
   private final CompletableFuture<TagsProvider.TagLookup<T>> parentProvider;
   protected final ResourceKey<? extends Registry<T>> registryKey;
   private final Map<Identifier, TagBuilder> builders = Maps.newLinkedHashMap();

   protected TagsProvider(
      final PackOutput output, final ResourceKey<? extends Registry<T>> registryKey, final CompletableFuture<HolderLookup.Provider> lookupProvider
   ) {
      this(output, registryKey, lookupProvider, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()));
   }

   protected TagsProvider(
      final PackOutput output,
      final ResourceKey<? extends Registry<T>> registryKey,
      final CompletableFuture<HolderLookup.Provider> lookupProvider,
      final CompletableFuture<TagsProvider.TagLookup<T>> parentProvider
   ) {
      this.pathProvider = output.createRegistryTagsPathProvider(registryKey);
      this.registryKey = registryKey;
      this.parentProvider = parentProvider;
      this.lookupProvider = lookupProvider;
   }

   @Override
   public final String getName() {
      return "Tags for " + this.registryKey.identifier();
   }

   protected abstract void addTags(HolderLookup.Provider registries);

   @Override
   public CompletableFuture<?> run(final CachedOutput cache) {
      record CombinedData<T>(HolderLookup.Provider contents, TagsProvider.TagLookup<T> parent) {
      }

      return this.createContentsProvider()
         .thenApply(provider -> {
            this.contentsDone.complete(null);
            return (HolderLookup.Provider)provider;
         })
         .thenCombineAsync(this.parentProvider, (x$0, x$1) -> new CombinedData<>(x$0, (TagsProvider.TagLookup<T>)x$1), Util.backgroundExecutor())
         .thenCompose(
            c -> {
               HolderLookup.RegistryLookup<T> lookup = c.contents.lookupOrThrow(this.registryKey);
               Predicate<Identifier> elementCheck = id -> lookup.get(ResourceKey.create(this.registryKey, id)).isPresent();
               Predicate<Identifier> tagCheck = id -> this.builders.containsKey(id) || c.parent.contains(TagKey.create(this.registryKey, id));
               return CompletableFuture.allOf(
                  this.builders
                     .entrySet()
                     .stream()
                     .map(
                        entry -> {
                           Identifier id = entry.getKey();
                           TagBuilder builder = entry.getValue();
                           List<TagEntry> entries = builder.build();
                           List<TagEntry> unresolvedEntries = entries.stream().filter(e -> !e.verifyIfPresent(elementCheck, tagCheck)).toList();
                           if (!unresolvedEntries.isEmpty()) {
                              throw new IllegalArgumentException(
                                 String.format(
                                    Locale.ROOT,
                                    "Couldn't define tag %s as it is missing following references: %s",
                                    id,
                                    unresolvedEntries.stream().map(Objects::toString).collect(Collectors.joining(","))
                                 )
                              );
                           }

                           Path path = this.pathProvider.json(id);
                           return DataProvider.saveStable(cache, c.contents, TagFile.CODEC, new TagFile(entries, builder.shouldReplace()), path);
                        }
                     )
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   protected TagBuilder getOrCreateRawBuilder(final TagKey<T> tag) {
      return this.builders.computeIfAbsent(tag.location(), k -> TagBuilder.create());
   }

   public CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter() {
      return this.contentsDone.thenApply(ignore -> id -> Optional.ofNullable(this.builders.get(id.location())));
   }

   protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
      return this.lookupProvider.thenApply(registries -> {
         this.builders.clear();
         this.addTags(registries);
         return (HolderLookup.Provider)registries;
      });
   }

   protected TagAppender<T> tag(final TagKey<T> tag) {
      TagBuilder builder = this.getOrCreateRawBuilder(tag);
      return TagAppender.forBuilder(builder);
   }

   protected TagAppender<T> tag(final TagKey<T> tag, final boolean replace) {
      TagBuilder builder = this.getOrCreateRawBuilder(tag);
      builder.setReplace(replace);
      return TagAppender.forBuilder(builder);
   }

   @FunctionalInterface
   public interface TagLookup<T> extends Function<TagKey<T>, Optional<TagBuilder>> {
      static <T> TagsProvider.TagLookup<T> empty() {
         return id -> Optional.empty();
      }

      default boolean contains(final TagKey<T> key) {
         return this.apply(key).isPresent();
      }
   }
}
