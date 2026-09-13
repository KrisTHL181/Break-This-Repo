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
      LayeredRegistryAccess<RegistryLayer> p_251774_
   ) {
      return RegistrySynchronization.networkSafeRegistries(p_251774_)
         .map(p_203949_ -> Pair.of(p_203949_.key(), serializeToNetwork(p_203949_.value())))
         .filter(p_358788_ -> !((TagNetworkSerialization.NetworkPayload)p_358788_.getSecond()).isEmpty())
         .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
   }

   private static <T> TagNetworkSerialization.NetworkPayload serializeToNetwork(Registry<T> p_203943_) {
      Map<Identifier, IntList> map = new HashMap<>();
      p_203943_.getTags().forEach(p_358791_ -> {
         IntList intlist = new IntArrayList(p_358791_.size());

         for (Holder<T> holder : p_358791_) {
            if (holder.kind() != Holder.Kind.REFERENCE) {
               throw new IllegalStateException("Can't serialize unregistered value " + holder);
            }

            intlist.add(p_203943_.getId(holder.value()));
         }

         map.put(p_358791_.key().location(), intlist);
      });
      return new TagNetworkSerialization.NetworkPayload(map);
   }

   static <T> TagLoader.LoadResult<T> deserializeTagsFromNetwork(Registry<T> p_203954_, TagNetworkSerialization.NetworkPayload p_203955_) {
      ResourceKey<? extends Registry<T>> resourcekey = p_203954_.key();
      Map<TagKey<T>, List<Holder<T>>> map = new HashMap<>();
      p_203955_.tags.forEach((p_449235_, p_449236_) -> {
         TagKey<T> tagkey = TagKey.create(resourcekey, p_449235_);
         List<Holder<T>> list = p_449236_.intStream().mapToObj(p_203954_::get).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
         map.put(tagkey, list);
      });
      return new TagLoader.LoadResult<>(resourcekey, map);
   }

   public static final class NetworkPayload {
      public static final TagNetworkSerialization.NetworkPayload EMPTY = new TagNetworkSerialization.NetworkPayload(Map.of());
      final Map<Identifier, IntList> tags;

      NetworkPayload(Map<Identifier, IntList> p_203965_) {
         this.tags = p_203965_;
      }

      public void write(FriendlyByteBuf p_203968_) {
         p_203968_.writeMap(this.tags, FriendlyByteBuf::writeIdentifier, FriendlyByteBuf::writeIntIdList);
      }

      public static TagNetworkSerialization.NetworkPayload read(FriendlyByteBuf p_203970_) {
         return new TagNetworkSerialization.NetworkPayload(p_203970_.readMap(FriendlyByteBuf::readIdentifier, FriendlyByteBuf::readIntIdList));
      }

      public boolean isEmpty() {
         return this.tags.isEmpty();
      }

      public int size() {
         return this.tags.size();
      }

      public <T> TagLoader.LoadResult<T> resolve(Registry<T> p_365168_) {
         return TagNetworkSerialization.deserializeTagsFromNetwork(p_365168_, this);
      }
   }
}
