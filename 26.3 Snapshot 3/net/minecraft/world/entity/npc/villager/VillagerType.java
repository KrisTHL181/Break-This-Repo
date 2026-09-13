package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public final class VillagerType {
   public static final ResourceKey<VillagerType> DESERT = createKey("desert");
   public static final ResourceKey<VillagerType> JUNGLE = createKey("jungle");
   public static final ResourceKey<VillagerType> PLAINS = createKey("plains");
   public static final ResourceKey<VillagerType> SAVANNA = createKey("savanna");
   public static final ResourceKey<VillagerType> SNOW = createKey("snow");
   public static final ResourceKey<VillagerType> SWAMP = createKey("swamp");
   public static final ResourceKey<VillagerType> TAIGA = createKey("taiga");
   public static final Codec<Holder<VillagerType>> CODEC = RegistryFixedCodec.create(Registries.VILLAGER_TYPE);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<VillagerType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.VILLAGER_TYPE);
   private static final Map<ResourceKey<Biome>, ResourceKey<VillagerType>> BY_BIOME = Util.make(Maps.newHashMap(), map -> {
      map.put(Biomes.BADLANDS, DESERT);
      map.put(Biomes.DESERT, DESERT);
      map.put(Biomes.ERODED_BADLANDS, DESERT);
      map.put(Biomes.WOODED_BADLANDS, DESERT);
      map.put(Biomes.BAMBOO_JUNGLE, JUNGLE);
      map.put(Biomes.JUNGLE, JUNGLE);
      map.put(Biomes.SPARSE_JUNGLE, JUNGLE);
      map.put(Biomes.SAVANNA_PLATEAU, SAVANNA);
      map.put(Biomes.SAVANNA, SAVANNA);
      map.put(Biomes.WINDSWEPT_SAVANNA, SAVANNA);
      map.put(Biomes.DEEP_FROZEN_OCEAN, SNOW);
      map.put(Biomes.FROZEN_OCEAN, SNOW);
      map.put(Biomes.FROZEN_RIVER, SNOW);
      map.put(Biomes.ICE_SPIKES, SNOW);
      map.put(Biomes.SNOWY_BEACH, SNOW);
      map.put(Biomes.SNOWY_TAIGA, SNOW);
      map.put(Biomes.SNOWY_PLAINS, SNOW);
      map.put(Biomes.GROVE, SNOW);
      map.put(Biomes.SNOWY_SLOPES, SNOW);
      map.put(Biomes.FROZEN_PEAKS, SNOW);
      map.put(Biomes.JAGGED_PEAKS, SNOW);
      map.put(Biomes.SWAMP, SWAMP);
      map.put(Biomes.MANGROVE_SWAMP, SWAMP);
      map.put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, TAIGA);
      map.put(Biomes.OLD_GROWTH_PINE_TAIGA, TAIGA);
      map.put(Biomes.WINDSWEPT_GRAVELLY_HILLS, TAIGA);
      map.put(Biomes.WINDSWEPT_HILLS, TAIGA);
      map.put(Biomes.TAIGA, TAIGA);
      map.put(Biomes.WINDSWEPT_FOREST, TAIGA);
   });

   private static ResourceKey<VillagerType> createKey(final String name) {
      return ResourceKey.create(Registries.VILLAGER_TYPE, Identifier.withDefaultNamespace(name));
   }

   private static VillagerType register(final Registry<VillagerType> registry, final ResourceKey<VillagerType> name) {
      return Registry.register(registry, name, new VillagerType());
   }

   public static VillagerType bootstrap(final Registry<VillagerType> registry) {
      register(registry, DESERT);
      register(registry, JUNGLE);
      register(registry, PLAINS);
      register(registry, SAVANNA);
      register(registry, SNOW);
      register(registry, SWAMP);
      return register(registry, TAIGA);
   }

   public static ResourceKey<VillagerType> byBiome(final Holder<Biome> biome) {
      return biome.unwrapKey().map(BY_BIOME::get).orElse(VillagerData.DEFAULT_TYPE);
   }
}
