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

   protected TagsProvider(PackOutput p_256596_, ResourceKey<? extends Registry<T>> p_255886_, CompletableFuture<HolderLookup.Provider> p_256513_) {
      this(p_256596_, p_255886_, p_256513_, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()));
   }

   protected TagsProvider(
      PackOutput p_275432_,
      ResourceKey<? extends Registry<T>> p_275476_,
      CompletableFuture<HolderLookup.Provider> p_275222_,
      CompletableFuture<TagsProvider.TagLookup<T>> p_275565_
   ) {
      this.pathProvider = p_275432_.createRegistryTagsPathProvider(p_275476_);
      this.registryKey = p_275476_;
      this.parentProvider = p_275565_;
      this.lookupProvider = p_275222_;
   }

   @Override
   public final String getName() {
      return "Tags for " + this.registryKey.identifier();
   }

   protected abstract void addTags(HolderLookup.Provider var1);

   @Override
   public CompletableFuture<?> run(CachedOutput p_253684_) {
      record CombinedData<T>(HolderLookup.Provider contents, TagsProvider.TagLookup<T> parent) {
      }

      return this.createContentsProvider()
         .thenApply(p_275895_ -> {
            this.contentsDone.complete(null);
            return (HolderLookup.Provider)p_275895_;
         })
         .thenCombineAsync(
            this.parentProvider, (p_274778_, p_274779_) -> new CombinedData<>(p_274778_, (TagsProvider.TagLookup<T>)p_274779_), Util.backgroundExecutor()
         )
         .thenCompose(
            p_325926_ -> {
               HolderLookup.RegistryLookup<T> registrylookup = p_325926_.contents.lookupOrThrow(this.registryKey);
               Predicate<Identifier> predicate = p_448741_ -> registrylookup.get(ResourceKey.create(this.registryKey, p_448741_)).isPresent();
               Predicate<Identifier> predicate1 = p_448739_ -> this.builders.containsKey(p_448739_)
                  || p_325926_.parent.contains(TagKey.create(this.registryKey, p_448739_));
               return CompletableFuture.allOf(
                  this.builders
                     .entrySet()
                     .stream()
                     .map(
                        p_325931_ -> {
                           Identifier identifier = p_325931_.getKey();
                           TagBuilder tagbuilder = p_325931_.getValue();
                           List<TagEntry> list = tagbuilder.build();
                           List<TagEntry> list1 = list.stream().filter(p_274771_ -> !p_274771_.verifyIfPresent(predicate, predicate1)).toList();
                           if (!list1.isEmpty()) {
                              throw new IllegalArgumentException(
                                 String.format(
                                    Locale.ROOT,
                                    "Couldn't define tag %s as it is missing following references: %s",
                                    identifier,
                                    list1.stream().map(Objects::toString).collect(Collectors.joining(","))
                                 )
                              );
                           }

                           Path path = this.pathProvider.json(identifier);
                           return DataProvider.saveStable(p_253684_, p_325926_.contents, TagFile.CODEC, new TagFile(list, false), path);
                        }
                     )
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   protected TagBuilder getOrCreateRawBuilder(TagKey<T> p_236452_) {
      return this.builders.computeIfAbsent(p_236452_.location(), p_460082_ -> TagBuilder.create());
   }

   public CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter() {
      return this.contentsDone.thenApply(p_276016_ -> p_448737_ -> Optional.ofNullable(this.builders.get(p_448737_.location())));
   }

   protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
      return this.lookupProvider.thenApply(p_274768_ -> {
         this.builders.clear();
         this.addTags(p_274768_);
         return (HolderLookup.Provider)p_274768_;
      });
   }

   @FunctionalInterface
   public interface TagLookup<T> extends Function<TagKey<T>, Optional<TagBuilder>> {
      static <T> TagsProvider.TagLookup<T> empty() {
         return p_275247_ -> Optional.empty();
      }

      default boolean contains(TagKey<T> p_275413_) {
         return this.apply(p_275413_).isPresent();
      }
   }
}
