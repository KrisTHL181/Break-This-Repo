package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class RegistrySetBuilder {
   private final List<RegistrySetBuilder.RegistryStub<?>> entries = new ArrayList<>();

   static <T> HolderGetter<T> wrapContextLookup(final HolderLookup.RegistryLookup<T> p_255625_) {
      return new RegistrySetBuilder.EmptyTagLookup<T>(p_255625_) {
         @Override
         public Optional<Holder.Reference<T>> get(ResourceKey<T> p_255765_) {
            return p_255625_.get(p_255765_);
         }
      };
   }

   static <T> HolderLookup.RegistryLookup<T> lookupFromMap(
      final ResourceKey<? extends Registry<? extends T>> p_311196_,
      final Lifecycle p_311352_,
      HolderOwner<T> p_335968_,
      final Map<ResourceKey<T>, Holder.Reference<T>> p_311458_
   ) {
      return new RegistrySetBuilder.EmptyTagRegistryLookup<T>(p_335968_) {
         @Override
         public ResourceKey<? extends Registry<? extends T>> key() {
            return p_311196_;
         }

         @Override
         public Lifecycle registryLifecycle() {
            return p_311352_;
         }

         @Override
         public Optional<Holder.Reference<T>> get(ResourceKey<T> p_310975_) {
            return Optional.ofNullable(p_311458_.get(p_310975_));
         }

         @Override
         public Stream<Holder.Reference<T>> listElements() {
            return p_311458_.values().stream();
         }
      };
   }

   public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> p_256446_, Lifecycle p_256394_, RegistrySetBuilder.RegistryBootstrap<T> p_256638_) {
      this.entries.add(new RegistrySetBuilder.RegistryStub<>(p_256446_, p_256394_, p_256638_));
      return this;
   }

   public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> p_256261_, RegistrySetBuilder.RegistryBootstrap<T> p_256010_) {
      return this.add(p_256261_, Lifecycle.stable(), p_256010_);
   }

   private RegistrySetBuilder.BuildState createState(RegistryAccess p_256400_) {
      RegistrySetBuilder.BuildState registrysetbuilder$buildstate = RegistrySetBuilder.BuildState.create(
         p_256400_, this.entries.stream().map(RegistrySetBuilder.RegistryStub::key)
      );
      this.entries.forEach(p_255629_ -> p_255629_.apply(registrysetbuilder$buildstate));
      return registrysetbuilder$buildstate;
   }

   private static HolderLookup.Provider buildProviderWithContext(
      RegistrySetBuilder.UniversalOwner p_328219_, RegistryAccess p_311176_, Stream<HolderLookup.RegistryLookup<?>> p_311668_
   ) {
      record Entry<T>(HolderLookup.RegistryLookup<T> lookup, RegistryOps.RegistryInfo<T> opsInfo) {
         public static <T> Entry<T> createForContextRegistry(HolderLookup.RegistryLookup<T> p_332859_) {
            return new Entry<>(new RegistrySetBuilder.EmptyTagLookupWrapper<>(p_332859_, p_332859_), RegistryOps.RegistryInfo.fromRegistryLookup(p_332859_));
         }

         public static <T> Entry<T> createForNewRegistry(RegistrySetBuilder.UniversalOwner p_329338_, HolderLookup.RegistryLookup<T> p_334381_) {
            return new Entry<>(
               new RegistrySetBuilder.EmptyTagLookupWrapper<>(p_329338_.cast(), p_334381_),
               new RegistryOps.RegistryInfo<>(p_329338_.cast(), p_334381_, p_334381_.registryLifecycle())
            );
         }
      }

      final Map<ResourceKey<? extends Registry<?>>, Entry<?>> map = new HashMap<>();
      p_311176_.registries().forEach(p_358096_ -> map.put(p_358096_.key(), Entry.createForContextRegistry(p_358096_.value())));
      p_311668_.forEach(p_325692_ -> map.put(p_325692_.key(), Entry.createForNewRegistry(p_328219_, p_325692_)));
      return new HolderLookup.Provider() {
         @Override
         public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
            return map.keySet().stream();
         }

         <T> Optional<Entry<T>> getEntry(ResourceKey<? extends Registry<? extends T>> p_332279_) {
            return Optional.ofNullable((Entry<T>)map.get(p_332279_));
         }

         @Override
         public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_328757_) {
            return this.getEntry(p_328757_).map(Entry::lookup);
         }

         @Override
         public <V> RegistryOps<V> createSerializationContext(DynamicOps<V> p_334886_) {
            return RegistryOps.create(p_334886_, new RegistryOps.RegistryInfoLookup() {
               @Override
               public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_331561_) {
                  return getEntry(p_331561_).map(Entry::opsInfo);
               }
            });
         }
      };
   }

   public HolderLookup.Provider build(RegistryAccess p_256112_) {
      RegistrySetBuilder.BuildState registrysetbuilder$buildstate = this.createState(p_256112_);
      Stream<HolderLookup.RegistryLookup<?>> stream = this.entries
         .stream()
         .map(p_325689_ -> p_325689_.collectRegisteredValues(registrysetbuilder$buildstate).buildAsLookup(registrysetbuilder$buildstate.owner));
      HolderLookup.Provider holderlookup$provider = buildProviderWithContext(registrysetbuilder$buildstate.owner, p_256112_, stream);
      registrysetbuilder$buildstate.reportNotCollectedHolders();
      registrysetbuilder$buildstate.reportUnclaimedRegisteredValues();
      registrysetbuilder$buildstate.throwOnError();
      return holderlookup$provider;
   }

   private HolderLookup.Provider createLazyFullPatchedRegistries(
      RegistryAccess p_312999_,
      HolderLookup.Provider p_309815_,
      Cloner.Factory p_311992_,
      Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryContents<?>> p_309672_,
      HolderLookup.Provider p_312434_
   ) {
      RegistrySetBuilder.UniversalOwner registrysetbuilder$universalowner = new RegistrySetBuilder.UniversalOwner();
      MutableObject<HolderLookup.Provider> mutableobject = new MutableObject();
      List<HolderLookup.RegistryLookup<?>> list = p_309672_.keySet()
         .stream()
         .map(
            p_308443_ -> this.createLazyFullPatchedRegistries(
               registrysetbuilder$universalowner, p_311992_, (ResourceKey<? extends Registry<? extends Object>>)p_308443_, p_312434_, p_309815_, mutableobject
            )
         )
         .collect(Collectors.toUnmodifiableList());
      HolderLookup.Provider holderlookup$provider = buildProviderWithContext(registrysetbuilder$universalowner, p_312999_, list.stream());
      mutableobject.setValue(holderlookup$provider);
      return holderlookup$provider;
   }

   private <T> HolderLookup.RegistryLookup<T> createLazyFullPatchedRegistries(
      HolderOwner<T> p_312548_,
      Cloner.Factory p_312934_,
      ResourceKey<? extends Registry<? extends T>> p_313093_,
      HolderLookup.Provider p_311682_,
      HolderLookup.Provider p_313198_,
      MutableObject<HolderLookup.Provider> p_311605_
   ) {
      Cloner<T> cloner = p_312934_.cloner(p_313093_);
      if (cloner == null) {
         throw new NullPointerException("No cloner for " + p_313093_.identifier());
      }

      Map<ResourceKey<T>, Holder.Reference<T>> map = new HashMap<>();
      HolderLookup.RegistryLookup<T> registrylookup = p_311682_.lookupOrThrow(p_313093_);
      registrylookup.listElements().forEach(p_308453_ -> {
         ResourceKey<T> resourcekey = p_308453_.key();
         RegistrySetBuilder.LazyHolder<T> lazyholder = new RegistrySetBuilder.LazyHolder<>(p_312548_, resourcekey);
         lazyholder.supplier = () -> cloner.clone((T)p_308453_.value(), p_311682_, (HolderLookup.Provider)p_311605_.get());
         map.put(resourcekey, lazyholder);
      });
      HolderLookup.RegistryLookup<T> registrylookup1 = p_313198_.lookupOrThrow(p_313093_);
      registrylookup1.listElements().forEach(p_308430_ -> {
         ResourceKey<T> resourcekey = p_308430_.key();
         map.computeIfAbsent(resourcekey, p_308437_ -> {
            RegistrySetBuilder.LazyHolder<T> lazyholder = new RegistrySetBuilder.LazyHolder<>(p_312548_, resourcekey);
            lazyholder.supplier = () -> cloner.clone((T)p_308430_.value(), p_313198_, (HolderLookup.Provider)p_311605_.get());
            return lazyholder;
         });
      });
      Lifecycle lifecycle = registrylookup.registryLifecycle().add(registrylookup1.registryLifecycle());
      return lookupFromMap(p_313093_, lifecycle, p_312548_, map);
   }

   public RegistrySetBuilder.PatchedRegistries buildPatch(RegistryAccess p_255676_, HolderLookup.Provider p_255900_, Cloner.Factory p_310265_) {
      RegistrySetBuilder.BuildState registrysetbuilder$buildstate = this.createState(p_255676_);
      Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryContents<?>> map = new HashMap<>();
      this.entries
         .stream()
         .map(p_308447_ -> p_308447_.collectRegisteredValues(registrysetbuilder$buildstate))
         .forEach(p_272339_ -> map.put(p_272339_.key, (RegistrySetBuilder.RegistryContents<?>)p_272339_));
      Set<ResourceKey<? extends Registry<?>>> set = p_255676_.listRegistryKeys().collect(Collectors.toUnmodifiableSet());
      p_255900_.listRegistryKeys()
         .filter(p_308455_ -> !set.contains(p_308455_))
         .forEach(
            p_308463_ -> map.putIfAbsent(
               (ResourceKey<? extends Registry<?>>)p_308463_,
               new RegistrySetBuilder.RegistryContents<>((ResourceKey<? extends Registry<?>>)p_308463_, Lifecycle.stable(), Map.of())
            )
         );
      Stream<HolderLookup.RegistryLookup<?>> stream = map.values().stream().map(p_325694_ -> p_325694_.buildAsLookup(registrysetbuilder$buildstate.owner));
      HolderLookup.Provider holderlookup$provider = buildProviderWithContext(registrysetbuilder$buildstate.owner, p_255676_, stream);
      registrysetbuilder$buildstate.reportUnclaimedRegisteredValues();
      registrysetbuilder$buildstate.throwOnError();
      HolderLookup.Provider holderlookup$provider1 = this.createLazyFullPatchedRegistries(p_255676_, p_255900_, p_310265_, map, holderlookup$provider);
      return new RegistrySetBuilder.PatchedRegistries(holderlookup$provider1, holderlookup$provider);
   }

   record BuildState(
      RegistrySetBuilder.UniversalOwner owner,
      RegistrySetBuilder.UniversalLookup lookup,
      Map<Identifier, HolderGetter<?>> registries,
      Map<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>> registeredValues,
      List<RuntimeException> errors
   ) {
      public static RegistrySetBuilder.BuildState create(RegistryAccess p_255995_, Stream<ResourceKey<? extends Registry<?>>> p_256495_) {
         RegistrySetBuilder.UniversalOwner registrysetbuilder$universalowner = new RegistrySetBuilder.UniversalOwner();
         List<RuntimeException> list = new ArrayList<>();
         RegistrySetBuilder.UniversalLookup registrysetbuilder$universallookup = new RegistrySetBuilder.UniversalLookup(registrysetbuilder$universalowner);
         Builder<Identifier, HolderGetter<?>> builder = ImmutableMap.builder();
         p_255995_.registries().forEach(p_448578_ -> builder.put(p_448578_.key().identifier(), RegistrySetBuilder.wrapContextLookup(p_448578_.value())));
         p_256495_.forEach(p_448576_ -> builder.put(p_448576_.identifier(), registrysetbuilder$universallookup));
         return new RegistrySetBuilder.BuildState(registrysetbuilder$universalowner, registrysetbuilder$universallookup, builder.build(), new HashMap<>(), list);
      }

      public <T> BootstrapContext<T> bootstrapContext() {
         return new BootstrapContext<T>() {
            @Override
            public Holder.Reference<T> register(ResourceKey<T> p_256176_, T p_256422_, Lifecycle p_255924_) {
               RegistrySetBuilder.RegisteredValue<?> registeredvalue = BuildState.this.registeredValues
                  .put(p_256176_, new RegistrySetBuilder.RegisteredValue(p_256422_, p_255924_));
               if (registeredvalue != null) {
                  BuildState.this.errors
                     .add(new IllegalStateException("Duplicate registration for " + p_256176_ + ", new=" + p_256422_ + ", old=" + registeredvalue.value));
               }

               return BuildState.this.lookup.getOrCreate(p_256176_);
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> p_255961_) {
               return (HolderGetter<S>)BuildState.this.registries.getOrDefault(p_255961_.identifier(), BuildState.this.lookup);
            }
         };
      }

      public void reportUnclaimedRegisteredValues() {
         this.registeredValues
            .forEach((p_325695_, p_325696_) -> this.errors.add(new IllegalStateException("Orpaned value " + p_325696_.value + " for key " + p_325695_)));
      }

      public void reportNotCollectedHolders() {
         for (ResourceKey<Object> resourcekey : this.lookup.holders.keySet()) {
            this.errors.add(new IllegalStateException("Unreferenced key: " + resourcekey));
         }
      }

      public void throwOnError() {
         if (!this.errors.isEmpty()) {
            IllegalStateException illegalstateexception = new IllegalStateException("Errors during registry creation");

            for (RuntimeException runtimeexception : this.errors) {
               illegalstateexception.addSuppressed(runtimeexception);
            }

            throw illegalstateexception;
         }
      }
   }

   abstract static class EmptyTagLookup<T> implements HolderGetter<T> {
      protected final HolderOwner<T> owner;

      protected EmptyTagLookup(HolderOwner<T> p_256166_) {
         this.owner = p_256166_;
      }

      @Override
      public Optional<HolderSet.Named<T>> get(TagKey<T> p_256664_) {
         return Optional.of(HolderSet.emptyNamed(this.owner, p_256664_));
      }
   }

   static class EmptyTagLookupWrapper<T> extends RegistrySetBuilder.EmptyTagRegistryLookup<T> implements HolderLookup.RegistryLookup.Delegate<T> {
      private final HolderLookup.RegistryLookup<T> parent;

      EmptyTagLookupWrapper(HolderOwner<T> p_327736_, HolderLookup.RegistryLookup<T> p_328715_) {
         super(p_327736_);
         this.parent = p_328715_;
      }

      @Override
      public HolderLookup.RegistryLookup<T> parent() {
         return this.parent;
      }
   }

   abstract static class EmptyTagRegistryLookup<T> extends RegistrySetBuilder.EmptyTagLookup<T> implements HolderLookup.RegistryLookup<T> {
      protected EmptyTagRegistryLookup(HolderOwner<T> p_334522_) {
         super(p_334522_);
      }

      @Override
      public Stream<HolderSet.Named<T>> listTags() {
         throw new UnsupportedOperationException("Tags are not available in datagen");
      }
   }

   static class LazyHolder<T> extends Holder.Reference<T> {
      @Nullable Supplier<T> supplier;

      protected LazyHolder(HolderOwner<T> p_311720_, @Nullable ResourceKey<T> p_312254_) {
         super(Holder.Reference.Type.STAND_ALONE, p_311720_, p_312254_, null);
      }

      @Override
      protected void bindValue(T p_309503_) {
         super.bindValue(p_309503_);
         this.supplier = null;
      }

      @Override
      public T value() {
         if (this.supplier != null) {
            this.bindValue(this.supplier.get());
         }

         return super.value();
      }
   }

   public record PatchedRegistries(HolderLookup.Provider full, HolderLookup.Provider patches) {
   }

   record RegisteredValue<T>(T value, Lifecycle lifecycle) {
   }

   @FunctionalInterface
   public interface RegistryBootstrap<T> {
      void run(BootstrapContext<T> var1);
   }

   record RegistryContents<T>(
      ResourceKey<? extends Registry<? extends T>> key, Lifecycle lifecycle, Map<ResourceKey<T>, RegistrySetBuilder.ValueAndHolder<T>> values
   ) {
      public HolderLookup.RegistryLookup<T> buildAsLookup(RegistrySetBuilder.UniversalOwner p_333021_) {
         Map<ResourceKey<T>, Holder.Reference<T>> map = this.values
            .entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(java.util.Map.Entry::getKey, p_311794_ -> {
               RegistrySetBuilder.ValueAndHolder<T> valueandholder = p_311794_.getValue();
               Holder.Reference<T> reference = valueandholder.holder().orElseGet(() -> Holder.Reference.createStandAlone(p_333021_.cast(), p_311794_.getKey()));
               reference.bindValue(valueandholder.value().value());
               return reference;
            }));
         return RegistrySetBuilder.lookupFromMap(this.key, this.lifecycle, p_333021_.cast(), map);
      }
   }

   record RegistryStub<T>(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, RegistrySetBuilder.RegistryBootstrap<T> bootstrap) {
      void apply(RegistrySetBuilder.BuildState p_256272_) {
         this.bootstrap.run(p_256272_.bootstrapContext());
      }

      public RegistrySetBuilder.RegistryContents<T> collectRegisteredValues(RegistrySetBuilder.BuildState p_256416_) {
         Map<ResourceKey<T>, RegistrySetBuilder.ValueAndHolder<T>> map = new HashMap<>();
         Iterator<java.util.Map.Entry<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>>> iterator = p_256416_.registeredValues.entrySet().iterator();

         while (iterator.hasNext()) {
            java.util.Map.Entry<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>> entry = iterator.next();
            ResourceKey<?> resourcekey = entry.getKey();
            if (resourcekey.isFor(this.key)) {
               RegistrySetBuilder.RegisteredValue<T> registeredvalue = (RegistrySetBuilder.RegisteredValue<T>)entry.getValue();
               Holder.Reference<T> reference = (Holder.Reference<T>)p_256416_.lookup.holders.remove(resourcekey);
               map.put((ResourceKey<T>)resourcekey, new RegistrySetBuilder.ValueAndHolder<>(registeredvalue, Optional.ofNullable(reference)));
               iterator.remove();
            }
         }

         return new RegistrySetBuilder.RegistryContents<>(this.key, this.lifecycle, map);
      }
   }

   static class UniversalLookup extends RegistrySetBuilder.EmptyTagLookup<Object> {
      final Map<ResourceKey<Object>, Holder.Reference<Object>> holders = new HashMap<>();

      public UniversalLookup(HolderOwner<Object> p_256629_) {
         super(p_256629_);
      }

      @Override
      public Optional<Holder.Reference<Object>> get(ResourceKey<Object> p_256303_) {
         return Optional.of(this.getOrCreate(p_256303_));
      }

      <T> Holder.Reference<T> getOrCreate(ResourceKey<T> p_256298_) {
         return (Holder.Reference<T>)this.holders
            .computeIfAbsent(p_256298_, p_256154_ -> Holder.Reference.createStandAlone(this.owner, (ResourceKey<Object>)p_256154_));
      }
   }

   static class UniversalOwner implements HolderOwner<Object> {
      public <T> HolderOwner<T> cast() {
         return this;
      }
   }

   record ValueAndHolder<T>(RegistrySetBuilder.RegisteredValue<T> value, Optional<Holder.Reference<T>> holder) {
   }
}
