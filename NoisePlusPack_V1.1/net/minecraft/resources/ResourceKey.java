package net.minecraft.resources;

import com.google.common.collect.MapMaker;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;

public class ResourceKey<T> {
   private static final ConcurrentMap<ResourceKey.InternKey, ResourceKey<?>> VALUES = new MapMaker().weakValues().makeMap();
   private final Identifier registryName;
   private final Identifier identifier;

   public static <T> Codec<ResourceKey<T>> codec(ResourceKey<? extends Registry<T>> p_195967_) {
      return Identifier.CODEC.xmap(p_448806_ -> create(p_195967_, p_448806_), ResourceKey::identifier);
   }

   public static <T> StreamCodec<ByteBuf, ResourceKey<T>> streamCodec(ResourceKey<? extends Registry<T>> p_335484_) {
      return Identifier.STREAM_CODEC.map(p_448804_ -> create(p_335484_, p_448804_), ResourceKey::identifier);
   }

   public static <T> ResourceKey<T> create(ResourceKey<? extends Registry<T>> p_457383_, Identifier p_454274_) {
      return create(p_457383_.identifier, p_454274_);
   }

   public static <T> ResourceKey<Registry<T>> createRegistryKey(Identifier p_458487_) {
      return create(Registries.ROOT_REGISTRY_NAME, p_458487_);
   }

   private static <T> ResourceKey<T> create(Identifier p_451826_, Identifier p_451643_) {
      return (ResourceKey<T>)VALUES.computeIfAbsent(
         new ResourceKey.InternKey(p_451826_, p_451643_), p_448802_ -> new ResourceKey(p_448802_.registry, p_448802_.identifier)
      );
   }

   private ResourceKey(Identifier p_456687_, Identifier p_457496_) {
      this.registryName = p_456687_;
      this.identifier = p_457496_;
   }

   @Override
   public String toString() {
      return "ResourceKey[" + this.registryName + " / " + this.identifier + "]";
   }

   public boolean isFor(ResourceKey<? extends Registry<?>> p_135784_) {
      return this.registryName.equals(p_135784_.identifier());
   }

   public <E> Optional<ResourceKey<E>> cast(ResourceKey<? extends Registry<E>> p_195976_) {
      return this.isFor(p_195976_) ? Optional.of((ResourceKey<E>)this) : Optional.empty();
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

   record InternKey(Identifier registry, Identifier identifier) {
   }
}
