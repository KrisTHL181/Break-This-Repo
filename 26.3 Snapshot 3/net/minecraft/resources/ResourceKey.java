package net.minecraft.resources;

import com.google.common.collect.MapMaker;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;

public class ResourceKey<T> {
   private static final ConcurrentMap<ResourceKey.InternKey, ResourceKey<?>> VALUES = new MapMaker().weakValues().makeMap();
   private final Identifier registryName;
   private final Identifier identifier;

   public static <T> Codec<ResourceKey<T>> codec(final ResourceKey<? extends Registry<T>> registryName) {
      return Identifier.CODEC.xmap(name -> create(registryName, name), ResourceKey::identifier);
   }

   public static <T> StreamCodec<ByteBuf, ResourceKey<T>> streamCodec(final ResourceKey<? extends Registry<T>> registryName) {
      return Identifier.STREAM_CODEC.map(name -> create(registryName, name), ResourceKey::identifier);
   }

   public static <T> ResourceKey<T> create(final ResourceKey<? extends Registry<T>> registryName, final Identifier location) {
      return create(registryName.identifier, location);
   }

   public static <T> ResourceKey<Registry<T>> createRegistryKey(final Identifier identifier) {
      return create(Registries.ROOT_REGISTRY_NAME, identifier);
   }

   private static <T> ResourceKey<T> create(final Identifier registryName, final Identifier identifier) {
      return (ResourceKey<T>)VALUES.computeIfAbsent(new ResourceKey.InternKey(registryName, identifier), k -> new ResourceKey(k.registry, k.identifier));
   }

   private ResourceKey(final Identifier registryName, final Identifier identifier) {
      this.registryName = registryName;
      this.identifier = identifier;
   }

   @Override
   public String toString() {
      return "ResourceKey[" + this.registryName + " / " + this.identifier + "]";
   }

   public boolean isFor(final ResourceKey<? extends Registry<?>> registry) {
      return this.registryName.equals(registry.identifier());
   }

   public <E> Optional<ResourceKey<E>> cast(final ResourceKey<? extends Registry<E>> registry) {
      return this.isFor(registry) ? Optional.of((ResourceKey<E>)this) : Optional.empty();
   }

   public <E> ResourceKey<E> dependent(final ResourceKey<? extends Registry<E>> registryKey, final String suffix) {
      return create(registryKey, this.identifier.withSuffix(suffix));
   }

   public <E> ResourceKey<E> dependent(final ResourceKey<? extends Registry<E>> registryKey, final UnaryOperator<String> decoration) {
      return create(registryKey, this.identifier.withPath(decoration));
   }

   public Identifier identifier() {
      return this.identifier;
   }

   public Identifier registry() {
      return this.registryName;
   }

   public ResourceKey<Registry<T>> registryKey() {
      return createRegistryKey(this.registryName);
   }

   private record InternKey(Identifier registry, Identifier identifier) {
   }
}
