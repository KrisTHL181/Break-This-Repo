package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class MappedRegistry<T> implements WritableRegistry<T> {
   private final ResourceKey<? extends Registry<T>> key;
   private final ObjectList<Holder.Reference<T>> byId = new ObjectArrayList(256);
   private final Reference2IntMap<T> toId = Util.make(new Reference2IntOpenHashMap(), p_308420_ -> p_308420_.defaultReturnValue(-1));
   private final Map<Identifier, Holder.Reference<T>> byLocation = new HashMap<>();
   private final Map<ResourceKey<T>, Holder.Reference<T>> byKey = new HashMap<>();
   private final Map<T, Holder.Reference<T>> byValue = new IdentityHashMap<>();
   private final Map<ResourceKey<T>, RegistrationInfo> registrationInfos = new IdentityHashMap<>();
   private Lifecycle registryLifecycle;
   private final Map<TagKey<T>, HolderSet.Named<T>> frozenTags = new IdentityHashMap<>();
   MappedRegistry.TagSet<T> allTags = MappedRegistry.TagSet.unbound();
   private boolean frozen;
   private @Nullable Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;

   @Override
   public Stream<HolderSet.Named<T>> listTags() {
      return this.getTags();
   }

   public MappedRegistry(ResourceKey<? extends Registry<T>> p_249899_, Lifecycle p_252249_) {
      this(p_249899_, p_252249_, false);
   }

   public MappedRegistry(ResourceKey<? extends Registry<T>> p_252132_, Lifecycle p_249215_, boolean p_251014_) {
      this.key = p_252132_;
      this.registryLifecycle = p_249215_;
      if (p_251014_) {
         this.unregisteredIntrusiveHolders = new IdentityHashMap<>();
      }
   }

   @Override
   public ResourceKey<? extends Registry<T>> key() {
      return this.key;
   }

   @Override
   public String toString() {
      return "Registry[" + this.key + " (" + this.registryLifecycle + ")]";
   }

   private void validateWrite() {
      if (this.frozen) {
         throw new IllegalStateException("Registry is already frozen");
      }
   }

   private void validateWrite(ResourceKey<T> p_205922_) {
      if (this.frozen) {
         throw new IllegalStateException("Registry is already frozen (trying to add key " + p_205922_ + ")");
      }
   }

   @Override
   public Holder.Reference<T> register(ResourceKey<T> p_256252_, T p_256591_, RegistrationInfo p_329661_) {
      this.validateWrite(p_256252_);
      Objects.requireNonNull(p_256252_);
      Objects.requireNonNull(p_256591_);
      if (this.byLocation.containsKey(p_256252_.identifier())) {
         throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Adding duplicate key '" + p_256252_ + "' to registry"));
      }

      if (this.byValue.containsKey(p_256591_)) {
         throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Adding duplicate value '" + p_256591_ + "' to registry"));
      }

      Holder.Reference<T> reference;
      if (this.unregisteredIntrusiveHolders != null) {
         reference = this.unregisteredIntrusiveHolders.remove(p_256591_);
         if (reference == null) {
            throw new AssertionError("Missing intrusive holder for " + p_256252_ + ":" + p_256591_);
         }

         reference.bindKey(p_256252_);
      } else {
         reference = this.byKey.computeIfAbsent(p_256252_, p_358082_ -> Holder.Reference.createStandAlone(this, (ResourceKey<T>)p_358082_));
      }

      this.byKey.put(p_256252_, reference);
      this.byLocation.put(p_256252_.identifier(), reference);
      this.byValue.put(p_256591_, reference);
      int i = this.byId.size();
      this.byId.add(reference);
      this.toId.put(p_256591_, i);
      this.registrationInfos.put(p_256252_, p_329661_);
      this.registryLifecycle = this.registryLifecycle.add(p_329661_.lifecycle());
      return reference;
   }

   @Override
   public @Nullable Identifier getKey(T p_122746_) {
      Holder.Reference<T> reference = this.byValue.get(p_122746_);
      return reference != null ? reference.key().identifier() : null;
   }

   @Override
   public Optional<ResourceKey<T>> getResourceKey(T p_122755_) {
      return Optional.ofNullable(this.byValue.get(p_122755_)).map(Holder.Reference::key);
   }

   @Override
   public int getId(@Nullable T p_122706_) {
      return this.toId.getInt(p_122706_);
   }

   @Override
   public @Nullable T getValue(@Nullable ResourceKey<T> p_122714_) {
      return getValueFromNullable(this.byKey.get(p_122714_));
   }

   @Override
   public @Nullable T byId(int p_122684_) {
      return (T)(p_122684_ >= 0 && p_122684_ < this.byId.size() ? ((Holder.Reference)this.byId.get(p_122684_)).value() : null);
   }

   @Override
   public Optional<Holder.Reference<T>> get(int p_205907_) {
      return p_205907_ >= 0 && p_205907_ < this.byId.size() ? Optional.ofNullable((Holder.Reference<T>)this.byId.get(p_205907_)) : Optional.empty();
   }

   @Override
   public Optional<Holder.Reference<T>> get(Identifier p_457466_) {
      return Optional.ofNullable(this.byLocation.get(p_457466_));
   }

   @Override
   public Optional<Holder.Reference<T>> get(ResourceKey<T> p_205905_) {
      return Optional.ofNullable(this.byKey.get(p_205905_));
   }

   @Override
   public Optional<Holder.Reference<T>> getAny() {
      return this.byId.isEmpty() ? Optional.empty() : Optional.of((Holder.Reference<T>)this.byId.getFirst());
   }

   @Override
   public Holder<T> wrapAsHolder(T p_263356_) {
      Holder.Reference<T> reference = this.byValue.get(p_263356_);
      return reference != null ? reference : Holder.direct(p_263356_);
   }

   Holder.Reference<T> getOrCreateHolderOrThrow(ResourceKey<T> p_248831_) {
      return this.byKey.computeIfAbsent(p_248831_, p_358081_ -> {
         if (this.unregisteredIntrusiveHolders != null) {
            throw new IllegalStateException("This registry can't create new holders without value");
         }

         this.validateWrite((ResourceKey<T>)p_358081_);
         return Holder.Reference.createStandAlone(this, (ResourceKey<T>)p_358081_);
      });
   }

   @Override
   public int size() {
      return this.byKey.size();
   }

   @Override
   public Optional<RegistrationInfo> registrationInfo(ResourceKey<T> p_331530_) {
      return Optional.ofNullable(this.registrationInfos.get(p_331530_));
   }

   @Override
   public Lifecycle registryLifecycle() {
      return this.registryLifecycle;
   }

   @Override
   public Iterator<T> iterator() {
      return Iterators.transform(this.byId.iterator(), Holder::value);
   }

   @Override
   public @Nullable T getValue(@Nullable Identifier p_459124_) {
      Holder.Reference<T> reference = this.byLocation.get(p_459124_);
      return getValueFromNullable(reference);
   }

   private static <T> @Nullable T getValueFromNullable(Holder.@Nullable Reference<T> p_205866_) {
      return p_205866_ != null ? p_205866_.value() : null;
   }

   @Override
   public Set<Identifier> keySet() {
      return Collections.unmodifiableSet(this.byLocation.keySet());
   }

   @Override
   public Set<ResourceKey<T>> registryKeySet() {
      return Collections.unmodifiableSet(this.byKey.keySet());
   }

   @Override
   public Set<Entry<ResourceKey<T>, T>> entrySet() {
      return Collections.unmodifiableSet(Util.<ResourceKey<T>, Holder.Reference<T>, T>mapValuesLazy(this.byKey, Holder::value).entrySet());
   }

   @Override
   public Stream<Holder.Reference<T>> listElements() {
      return this.byId.stream();
   }

   @Override
   public Stream<HolderSet.Named<T>> getTags() {
      return this.allTags.getTags();
   }

   HolderSet.Named<T> getOrCreateTagForRegistration(TagKey<T> p_363833_) {
      return this.frozenTags.computeIfAbsent(p_363833_, this::createTag);
   }

   private HolderSet.Named<T> createTag(TagKey<T> p_211068_) {
      return new HolderSet.Named<>(this, p_211068_);
   }

   @Override
   public boolean isEmpty() {
      return this.byKey.isEmpty();
   }

   @Override
   public Optional<Holder.Reference<T>> getRandom(RandomSource p_235716_) {
      return Util.getRandomSafe(this.byId, p_235716_);
   }

   @Override
   public boolean containsKey(Identifier p_452771_) {
      return this.byLocation.containsKey(p_452771_);
   }

   @Override
   public boolean containsKey(ResourceKey<T> p_175392_) {
      return this.byKey.containsKey(p_175392_);
   }

   @Override
   public Registry<T> freeze() {
      if (this.frozen) {
         return this;
      }

      this.frozen = true;
      this.byValue.forEach((p_247989_, p_247990_) -> p_247990_.bindValue((T)p_247989_));
      List<Identifier> list = this.byKey
         .entrySet()
         .stream()
         .filter(p_211055_ -> !p_211055_.getValue().isBound())
         .map(p_448560_ -> p_448560_.getKey().identifier())
         .sorted()
         .toList();
      if (!list.isEmpty()) {
         throw new IllegalStateException("Unbound values in registry " + this.key() + ": " + list);
      }

      if (this.unregisteredIntrusiveHolders != null) {
         if (!this.unregisteredIntrusiveHolders.isEmpty()) {
            throw new IllegalStateException("Some intrusive holders were not registered: " + this.unregisteredIntrusiveHolders.values());
         }

         this.unregisteredIntrusiveHolders = null;
      }

      if (this.allTags.isBound()) {
         throw new IllegalStateException("Tags already present before freezing");
      }

      List<Identifier> list1 = this.frozenTags
         .entrySet()
         .stream()
         .filter(p_358086_ -> !p_358086_.getValue().isBound())
         .map(p_448561_ -> p_448561_.getKey().location())
         .sorted()
         .toList();
      if (!list1.isEmpty()) {
         throw new IllegalStateException("Unbound tags in registry " + this.key() + ": " + list1);
      }

      this.allTags = MappedRegistry.TagSet.fromMap(this.frozenTags);
      this.refreshTagsInHolders();
      return this;
   }

   @Override
   public Holder.Reference<T> createIntrusiveHolder(T p_205915_) {
      if (this.unregisteredIntrusiveHolders == null) {
         throw new IllegalStateException("This registry can't create intrusive holders");
      }

      this.validateWrite();
      return this.unregisteredIntrusiveHolders.computeIfAbsent(p_205915_, p_358092_ -> Holder.Reference.createIntrusive(this, (T)p_358092_));
   }

   @Override
   public Optional<HolderSet.Named<T>> get(TagKey<T> p_365729_) {
      return this.allTags.get(p_365729_);
   }

   private Holder.Reference<T> validateAndUnwrapTagElement(TagKey<T> p_363318_, Holder<T> p_362647_) {
      if (!p_362647_.canSerializeIn(this)) {
         throw new IllegalStateException("Can't create named set " + p_363318_ + " containing value " + p_362647_ + " from outside registry " + this);
      } else if (p_362647_ instanceof Holder.Reference<T> reference) {
         return reference;
      } else {
         throw new IllegalStateException("Found direct holder " + p_362647_ + " value in tag " + p_363318_);
      }
   }

   @Override
   public void bindTag(TagKey<T> p_361998_, List<Holder<T>> p_361891_) {
      this.validateWrite();
      this.getOrCreateTagForRegistration(p_361998_).bind(p_361891_);
   }

   void refreshTagsInHolders() {
      Map<Holder.Reference<T>, List<TagKey<T>>> map = new IdentityHashMap<>();
      this.byKey.values().forEach(p_211801_ -> map.put((Holder.Reference<T>)p_211801_, new ArrayList<>()));
      this.allTags.forEach((p_358084_, p_358085_) -> {
         for (Holder<T> holder : p_358085_) {
            Holder.Reference<T> reference = this.validateAndUnwrapTagElement((TagKey<T>)p_358084_, holder);
            map.get(reference).add((TagKey<T>)p_358084_);
         }
      });
      map.forEach(Holder.Reference::bindTags);
   }

   public void bindAllTagsToEmpty() {
      this.validateWrite();
      this.frozenTags.values().forEach(p_211792_ -> p_211792_.bind(List.of()));
   }

   @Override
   public HolderGetter<T> createRegistrationLookup() {
      this.validateWrite();
      return new HolderGetter<T>() {
         @Override
         public Optional<Holder.Reference<T>> get(ResourceKey<T> p_255624_) {
            return Optional.of(this.getOrThrow(p_255624_));
         }

         @Override
         public Holder.Reference<T> getOrThrow(ResourceKey<T> p_362418_) {
            return MappedRegistry.this.getOrCreateHolderOrThrow(p_362418_);
         }

         @Override
         public Optional<HolderSet.Named<T>> get(TagKey<T> p_256277_) {
            return Optional.of(this.getOrThrow(p_256277_));
         }

         @Override
         public HolderSet.Named<T> getOrThrow(TagKey<T> p_365508_) {
            return MappedRegistry.this.getOrCreateTagForRegistration(p_365508_);
         }
      };
   }

   @Override
   public Registry.PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> p_368827_) {
      if (!this.frozen) {
         throw new IllegalStateException("Invalid method used for tag loading");
      }

      Builder<TagKey<T>, HolderSet.Named<T>> builder = ImmutableMap.builder();
      final Map<TagKey<T>, List<Holder<T>>> map = new HashMap<>();
      p_368827_.tags().forEach((p_358090_, p_358091_) -> {
         HolderSet.Named<T> named = this.frozenTags.get(p_358090_);
         if (named == null) {
            named = this.createTag((TagKey<T>)p_358090_);
         }

         builder.put(p_358090_, named);
         map.put((TagKey<T>)p_358090_, List.copyOf(p_358091_));
      });
      final ImmutableMap<TagKey<T>, HolderSet.Named<T>> immutablemap = builder.build();
      final HolderLookup.RegistryLookup<T> registrylookup = new HolderLookup.RegistryLookup.Delegate<T>() {
         @Override
         public HolderLookup.RegistryLookup<T> parent() {
            return MappedRegistry.this;
         }

         @Override
         public Optional<HolderSet.Named<T>> get(TagKey<T> p_259486_) {
            return Optional.ofNullable((HolderSet.Named<T>)immutablemap.get(p_259486_));
         }

         @Override
         public Stream<HolderSet.Named<T>> listTags() {
            return immutablemap.values().stream();
         }
      };
      return new Registry.PendingTags<T>() {
         @Override
         public ResourceKey<? extends Registry<? extends T>> key() {
            return MappedRegistry.this.key();
         }

         @Override
         public int size() {
            return map.size();
         }

         @Override
         public HolderLookup.RegistryLookup<T> lookup() {
            return registrylookup;
         }

         @Override
         public void apply() {
            immutablemap.forEach((p_363053_, p_363988_) -> {
               List<Holder<T>> list = map.getOrDefault(p_363053_, List.of());
               p_363988_.bind(list);
            });
            MappedRegistry.this.allTags = MappedRegistry.TagSet.fromMap(immutablemap);
            MappedRegistry.this.refreshTagsInHolders();
         }
      };
   }

   interface TagSet<T> {
      static <T> MappedRegistry.TagSet<T> unbound() {
         return new MappedRegistry.TagSet<T>() {
            @Override
            public boolean isBound() {
               return false;
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> p_369929_) {
               throw new IllegalStateException("Tags not bound, trying to access " + p_369929_);
            }

            @Override
            public void forEach(BiConsumer<? super TagKey<T>, ? super HolderSet.Named<T>> p_367948_) {
               throw new IllegalStateException("Tags not bound");
            }

            @Override
            public Stream<HolderSet.Named<T>> getTags() {
               throw new IllegalStateException("Tags not bound");
            }
         };
      }

      static <T> MappedRegistry.TagSet<T> fromMap(final Map<TagKey<T>, HolderSet.Named<T>> p_364506_) {
         return new MappedRegistry.TagSet<T>() {
            @Override
            public boolean isBound() {
               return true;
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> p_364348_) {
               return Optional.ofNullable(p_364506_.get(p_364348_));
            }

            @Override
            public void forEach(BiConsumer<? super TagKey<T>, ? super HolderSet.Named<T>> p_366521_) {
               p_364506_.forEach(p_366521_);
            }

            @Override
            public Stream<HolderSet.Named<T>> getTags() {
               return p_364506_.values().stream();
            }
         };
      }

      boolean isBound();

      Optional<HolderSet.Named<T>> get(TagKey<T> var1);

      void forEach(BiConsumer<? super TagKey<T>, ? super HolderSet.Named<T>> var1);

      Stream<HolderSet.Named<T>> getTags();
   }
}
