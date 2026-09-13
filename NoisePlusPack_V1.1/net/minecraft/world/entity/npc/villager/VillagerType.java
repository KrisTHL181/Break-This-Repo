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
   private static final Map<ResourceKey<Biome>, ResourceKey<VillagerType>> BY_BIOME = Util.make(Maps.newHashMap(), p_453830_ -> {
      p_453830_.put(Biomes.BADLANDS, DESERT);
      p_453830_.put(Biomes.DESERT, DESERT);
      p_453830_.put(Biomes.ERODED_BADLANDS, DESERT);
      p_453830_.put(Biomes.WOODED_BADLANDS, DESERT);
      p_453830_.put(Biomes.BAMBOO_JUNGLE, JUNGLE);
      p_453830_.put(Biomes.JUNGLE, JUNGLE);
      p_453830_.put(Biomes.SPARSE_JUNGLE, JUNGLE);
      p_453830_.put(Biomes.SAVANNA_PLATEAU, SAVANNA);
      p_453830_.put(Biomes.SAVANNA, SAVANNA);
      p_453830_.put(Biomes.WINDSWEPT_SAVANNA, SAVANNA);
      p_453830_.put(Biomes.DEEP_FROZEN_OCEAN, SNOW);
      p_453830_.put(Biomes.FROZEN_OCEAN, SNOW);
      p_453830_.put(Biomes.FROZEN_RIVER, SNOW);
      p_453830_.put(Biomes.ICE_SPIKES, SNOW);
      p_453830_.put(Biomes.SNOWY_BEACH, SNOW);
      p_453830_.put(Biomes.SNOWY_TAIGA, SNOW);
      p_453830_.put(Biomes.SNOWY_PLAINS, SNOW);
      p_453830_.put(Biomes.GROVE, SNOW);
      p_453830_.put(Biomes.SNOWY_SLOPES, SNOW);
      p_453830_.put(Biomes.FROZEN_PEAKS, SNOW);
      p_453830_.put(Biomes.JAGGED_PEAKS, SNOW);
      p_453830_.put(Biomes.SWAMP, SWAMP);
      p_453830_.put(Biomes.MANGROVE_SWAMP, SWAMP);
      p_453830_.put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, TAIGA);
      p_453830_.put(Biomes.OLD_GROWTH_PINE_TAIGA, TAIGA);
      p_453830_.put(Biomes.WINDSWEPT_GRAVELLY_HILLS, TAIGA);
      p_453830_.put(Biomes.WINDSWEPT_HILLS, TAIGA);
      p_453830_.put(Biomes.TAIGA, TAIGA);
      p_453830_.put(Biomes.WINDSWEPT_FOREST, TAIGA);
   });

   private static ResourceKey<VillagerType> createKey(String p_452354_) {
      return ResourceKey.create(Registries.VILLAGER_TYPE, Identifier.withDefaultNamespace(p_452354_));
   }

   private static VillagerType register(Registry<VillagerType> p_458743_, ResourceKey<VillagerType> p_453191_) {
      return Registry.register(p_458743_, p_453191_, new VillagerType());
   }

   public static VillagerType bootstrap(Registry<VillagerType> p_455007_) {
      register(p_455007_, DESERT);
      register(p_455007_, JUNGLE);
      register(p_455007_, PLAINS);
      register(p_455007_, SAVANNA);
      register(p_455007_, SNOW);
      register(p_455007_, SWAMP);
      return register(p_455007_, TAIGA);
   }

   public static ResourceKey<VillagerType> byBiome(Holder<Biome> p_452045_) {
      return p_452045_.unwrapKey().map(BY_BIOME::get).orElse(PLAINS);
   }
}
