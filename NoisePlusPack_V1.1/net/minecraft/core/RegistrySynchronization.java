package net.minecraft.core;

import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.repository.KnownPack;

public class RegistrySynchronization {
   private static final Set<ResourceKey<? extends Registry<?>>> NETWORKABLE_REGISTRIES = RegistryDataLoader.SYNCHRONIZED_REGISTRIES
      .stream()
      .map(RegistryDataLoader.RegistryData::key)
      .collect(Collectors.toUnmodifiableSet());

   public static void packRegistries(
      DynamicOps<Tag> p_330752_,
      RegistryAccess p_332359_,
      Set<KnownPack> p_331327_,
      BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> p_335166_
   ) {
      RegistryDataLoader.SYNCHRONIZED_REGISTRIES
         .forEach(p_325710_ -> packRegistry(p_330752_, (RegistryDataLoader.RegistryData<?>)p_325710_, p_332359_, p_331327_, p_335166_));
   }

   private static <T> void packRegistry(
      DynamicOps<Tag> p_328835_,
      RegistryDataLoader.RegistryData<T> p_329218_,
      RegistryAccess p_335981_,
      Set<KnownPack> p_330196_,
      BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> p_330046_
   ) {
      p_335981_.lookup(p_329218_.key())
         .ifPresent(
            p_358104_ -> {
               List<RegistrySynchronization.PackedRegistryEntry> list = new ArrayList<>(p_358104_.size());
               p_358104_.listElements()
                  .forEach(
                     p_325717_ -> {
                        boolean flag = p_358104_.registrationInfo(p_325717_.key())
                           .flatMap(RegistrationInfo::knownPackInfo)
                           .filter(p_330196_::contains)
                           .isPresent();
                        Optional<Tag> optional;
                        if (flag) {
                           optional = Optional.empty();
                        } else {
                           Tag tag = (Tag)p_329218_.elementCodec()
                              .encodeStart(p_328835_, p_325717_.value())
                              .getOrThrow(p_325700_ -> new IllegalArgumentException("Failed to serialize " + p_325717_.key() + ": " + p_325700_));
                           optional = Optional.of(tag);
                        }

                        list.add(new RegistrySynchronization.PackedRegistryEntry(p_325717_.key().identifier(), optional));
                     }
                  );
               p_330046_.accept(p_358104_.key(), list);
            }
         );
   }

   private static Stream<RegistryAccess.RegistryEntry<?>> ownedNetworkableRegistries(RegistryAccess p_251842_) {
      return p_251842_.registries().filter(p_358099_ -> isNetworkable(p_358099_.key()));
   }

   public static Stream<RegistryAccess.RegistryEntry<?>> networkedRegistries(LayeredRegistryAccess<RegistryLayer> p_259290_) {
      return ownedNetworkableRegistries(p_259290_.getAccessFrom(RegistryLayer.WORLDGEN));
   }

   public static Stream<RegistryAccess.RegistryEntry<?>> networkSafeRegistries(LayeredRegistryAccess<RegistryLayer> p_249066_) {
      Stream<RegistryAccess.RegistryEntry<?>> stream = p_249066_.getLayer(RegistryLayer.STATIC).registries();
      Stream<RegistryAccess.RegistryEntry<?>> stream1 = networkedRegistries(p_249066_);
      return Stream.concat(stream1, stream);
   }

   public static boolean isNetworkable(ResourceKey<? extends Registry<?>> p_362141_) {
      return NETWORKABLE_REGISTRIES.contains(p_362141_);
   }

   public record PackedRegistryEntry(Identifier id, Optional<Tag> data) {
      public static final StreamCodec<ByteBuf, RegistrySynchronization.PackedRegistryEntry> STREAM_CODEC = StreamCodec.composite(
         Identifier.STREAM_CODEC,
         RegistrySynchronization.PackedRegistryEntry::id,
         ByteBufCodecs.TAG.apply(ByteBufCodecs::optional),
         RegistrySynchronization.PackedRegistryEntry::data,
         RegistrySynchronization.PackedRegistryEntry::new
      );
   }
}
