package net.minecraft.tags;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import net.minecraft.util.StrictJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TagLoader<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   final TagLoader.ElementLookup<T> elementLookup;
   private final String directory;

   public TagLoader(TagLoader.ElementLookup<T> p_365781_, String p_144494_) {
      this.elementLookup = p_365781_;
      this.directory = p_144494_;
   }

   public Map<Identifier, List<TagLoader.EntryWithSource>> load(ResourceManager p_144496_) {
      Map<Identifier, List<TagLoader.EntryWithSource>> map = new HashMap<>();
      FileToIdConverter filetoidconverter = FileToIdConverter.json(this.directory);

      for (Entry<Identifier, List<Resource>> entry : filetoidconverter.listMatchingResourceStacks(p_144496_).entrySet()) {
         Identifier identifier = entry.getKey();
         Identifier identifier1 = filetoidconverter.fileToId(identifier);

         for (Resource resource : entry.getValue()) {
            try (Reader reader = resource.openAsReader()) {
               JsonElement jsonelement = StrictJsonParser.parse(reader);
               List<TagLoader.EntryWithSource> list = map.computeIfAbsent(identifier1, p_451223_ -> new ArrayList<>());
               TagFile tagfile = (TagFile)TagFile.CODEC.parse(new Dynamic(JsonOps.INSTANCE, jsonelement)).getOrThrow();
               if (tagfile.replace()) {
                  list.clear();
               }

               String s = resource.sourcePackId();
               tagfile.entries().forEach(p_215997_ -> list.add(new TagLoader.EntryWithSource(p_215997_, s)));
            } catch (Exception exception) {
               LOGGER.error("Couldn't read tag list {} from {} in data pack {}", new Object[]{identifier1, identifier, resource.sourcePackId(), exception});
            }
         }
      }

      return map;
   }

   private Either<List<TagLoader.EntryWithSource>, List<T>> tryBuildTag(TagEntry.Lookup<T> p_215979_, List<TagLoader.EntryWithSource> p_215980_) {
      SequencedSet<T> sequencedset = new LinkedHashSet<>();
      List<TagLoader.EntryWithSource> list = new ArrayList<>();

      for (TagLoader.EntryWithSource tagloader$entrywithsource : p_215980_) {
         if (!tagloader$entrywithsource.entry().build(p_215979_, sequencedset::add)) {
            list.add(tagloader$entrywithsource);
         }
      }

      return list.isEmpty() ? Either.right(List.copyOf(sequencedset)) : Either.left(list);
   }

   public Map<Identifier, List<T>> build(Map<Identifier, List<TagLoader.EntryWithSource>> p_203899_) {
      final Map<Identifier, List<T>> map = new HashMap<>();
      TagEntry.Lookup<T> lookup = new TagEntry.Lookup<T>() {
         @Override
         public @Nullable T element(Identifier p_456898_, boolean p_366980_) {
            return (T)TagLoader.this.elementLookup.get(p_456898_, p_366980_).orElse(null);
         }

         @Override
         public @Nullable Collection<T> tag(Identifier p_457870_) {
            return map.get(p_457870_);
         }
      };
      DependencySorter<Identifier, TagLoader.SortingEntry> dependencysorter = new DependencySorter<>();
      p_203899_.forEach((p_450289_, p_284686_) -> dependencysorter.addEntry(p_450289_, new TagLoader.SortingEntry((List<TagLoader.EntryWithSource>)p_284686_)));
      dependencysorter.orderByDependencies(
         (p_450305_, p_358781_) -> this.tryBuildTag(lookup, p_358781_.entries)
            .ifLeft(
               p_358772_ -> LOGGER.error(
                  "Couldn't load tag {} as it is missing following references: {}",
                  p_450305_,
                  p_358772_.stream().map(Objects::toString).collect(Collectors.joining(", "))
               )
            )
            .ifRight(p_369415_ -> map.put(p_450305_, p_369415_))
      );
      return map;
   }

   public static <T> void loadTagsFromNetwork(TagNetworkSerialization.NetworkPayload p_363340_, WritableRegistry<T> p_362274_) {
      p_363340_.resolve(p_362274_).tags.forEach(p_362274_::bindTag);
   }

   public static List<Registry.PendingTags<?>> loadTagsForExistingRegistries(ResourceManager p_363516_, RegistryAccess p_365200_) {
      return p_365200_.registries()
         .map(p_358777_ -> loadPendingTags(p_363516_, p_358777_.value()))
         .flatMap(Optional::stream)
         .collect(Collectors.toUnmodifiableList());
   }

   public static <T> void loadTagsForRegistry(ResourceManager p_361002_, WritableRegistry<T> p_369889_) {
      ResourceKey<? extends Registry<T>> resourcekey = p_369889_.key();
      TagLoader<Holder<T>> tagloader = new TagLoader<>(TagLoader.ElementLookup.fromWritableRegistry(p_369889_), Registries.tagsDirPath(resourcekey));
      tagloader.build(tagloader.load(p_361002_))
         .forEach((p_449223_, p_449224_) -> p_369889_.bindTag(TagKey.create(resourcekey, p_449223_), (List<Holder<T>>)p_449224_));
   }

   private static <T> Map<TagKey<T>, List<Holder<T>>> wrapTags(ResourceKey<? extends Registry<T>> p_369888_, Map<Identifier, List<Holder<T>>> p_362414_) {
      return p_362414_.entrySet().stream().collect(Collectors.toUnmodifiableMap(p_449220_ -> TagKey.create(p_369888_, p_449220_.getKey()), Entry::getValue));
   }

   private static <T> Optional<Registry.PendingTags<T>> loadPendingTags(ResourceManager p_366215_, Registry<T> p_369074_) {
      ResourceKey<? extends Registry<T>> resourcekey = p_369074_.key();
      TagLoader<Holder<T>> tagloader = new TagLoader<>(
         (TagLoader.ElementLookup<Holder<T>>)TagLoader.ElementLookup.fromFrozenRegistry(p_369074_), Registries.tagsDirPath(resourcekey)
      );
      TagLoader.LoadResult<T> loadresult = new TagLoader.LoadResult<>(resourcekey, wrapTags(p_369074_.key(), tagloader.build(tagloader.load(p_366215_))));
      return loadresult.tags().isEmpty() ? Optional.empty() : Optional.of(p_369074_.prepareTagReload(loadresult));
   }

   public static List<HolderLookup.RegistryLookup<?>> buildUpdatedLookups(RegistryAccess.Frozen p_361092_, List<Registry.PendingTags<?>> p_361987_) {
      List<HolderLookup.RegistryLookup<?>> list = new ArrayList<>();
      p_361092_.registries().forEach(p_358775_ -> {
         Registry.PendingTags<?> pendingtags = findTagsForRegistry(p_361987_, p_358775_.key());
         list.add(pendingtags != null ? pendingtags.lookup() : p_358775_.value());
      });
      return list;
   }

   private static Registry.@Nullable PendingTags<?> findTagsForRegistry(List<Registry.PendingTags<?>> p_361794_, ResourceKey<? extends Registry<?>> p_361930_) {
      for (Registry.PendingTags<?> pendingtags : p_361794_) {
         if (pendingtags.key() == p_361930_) {
            return pendingtags;
         }
      }

      return null;
   }

   public interface ElementLookup<T> {
      Optional<? extends T> get(Identifier var1, boolean var2);

      static <T> TagLoader.ElementLookup<? extends Holder<T>> fromFrozenRegistry(Registry<T> p_369869_) {
         return (p_449230_, p_449231_) -> p_369869_.get(p_449230_);
      }

      static <T> TagLoader.ElementLookup<Holder<T>> fromWritableRegistry(WritableRegistry<T> p_361559_) {
         HolderGetter<T> holdergetter = p_361559_.createRegistrationLookup();
         return (p_449227_, p_449228_) -> (p_449228_ ? holdergetter : p_361559_).get(ResourceKey.create(p_361559_.key(), p_449227_));
      }
   }

   public record EntryWithSource(TagEntry entry, String source) {
      @Override
      public String toString() {
         return this.entry + " (from " + this.source + ")";
      }
   }

   public record LoadResult<T>(ResourceKey<? extends Registry<T>> key, Map<TagKey<T>, List<Holder<T>>> tags) {
   }

   record SortingEntry(List<TagLoader.EntryWithSource> entries) implements DependencySorter.Entry<Identifier> {
      @Override
      public void visitRequiredDependencies(Consumer<Identifier> p_285529_) {
         this.entries.forEach(p_285236_ -> p_285236_.entry.visitRequiredDependencies(p_285529_));
      }

      @Override
      public void visitOptionalDependencies(Consumer<Identifier> p_285469_) {
         this.entries.forEach(p_284943_ -> p_284943_.entry.visitOptionalDependencies(p_285469_));
      }
   }
}
