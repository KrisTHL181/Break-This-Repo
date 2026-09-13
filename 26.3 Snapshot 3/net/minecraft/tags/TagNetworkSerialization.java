package net.minecraft.tags;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;

public class TagNetworkSerialization {
   public static Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> serializeTagsToNetwork(
      final LayeredRegistryAccess<RegistryLayer> registries
   ) {
      return RegistrySynchronization.networkSafeRegistries(registries)
         .map(e -> Pair.of(e.key(), serializeToNetwork(e.value())))
         .filter(e -> !((TagNetworkSerialization.NetworkPayload)e.getSecond()).isEmpty())
         .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
   }

   private static <T> TagNetworkSerialization.NetworkPayload serializeToNetwork(final Registry<T> registry) {
      Map<Identifier, IntList> result = new HashMap<>();
      registry.getTags().forEach(tag -> {
         IntList ids = new IntArrayList(tag.size());

         for (Holder<T> holder : tag) {
            if (holder.kind() != Holder.Kind.REFERENCE) {
               throw new IllegalStateException("Can't serialize unregistered value " + holder);
            }

            ids.add(registry.getId(holder.value()));
         }

         result.put(tag.key().location(), ids);
      });
      return new TagNetworkSerialization.NetworkPayload(result);
   }

   private static <T> TagLoader.LoadResult<T> deserializeTagsFromNetwork(final Registry<T> registry, final TagNetworkSerialization.NetworkPayload payload) {
      ResourceKey<? extends Registry<T>> registryKey = registry.key();
      Map<TagKey<T>, List<Holder<T>>> tags = new HashMap<>();
      payload.tags.forEach((key, ids) -> {
         TagKey<T> tagKey = TagKey.create(registryKey, key);
         List<Holder<T>> values = ids.intStream().mapToObj(registry::get).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
         tags.put(tagKey, values);
      });
      return new TagLoader.LoadResult<>(registryKey, tags);
   }

   public static final class NetworkPayload {
      public static final TagNetworkSerialization.NetworkPayload EMPTY = new TagNetworkSerialization.NetworkPayload(Map.of());
      private final Map<Identifier, IntList> tags;

      public NetworkPayload(final Map<Identifier, IntList> tags) {
         this.tags = tags;
      }

      public void write(final FriendlyByteBuf buf) {
         buf.writeMap(this.tags, FriendlyByteBuf::writeIdentifier, FriendlyByteBuf::writeIntIdList);
      }

      public static TagNetworkSerialization.NetworkPayload read(final FriendlyByteBuf buf) {
         return new TagNetworkSerialization.NetworkPayload(buf.readMap(FriendlyByteBuf::readIdentifier, FriendlyByteBuf::readIntIdList));
      }

      public boolean isEmpty() {
         return this.tags.isEmpty();
      }

      public int size() {
         return this.tags.size();
      }

      public <T> TagLoader.LoadResult<T> resolve(final Registry<T> registry) {
         return TagNetworkSerialization.deserializeTagsFromNetwork(registry, this);
      }
   }
}
