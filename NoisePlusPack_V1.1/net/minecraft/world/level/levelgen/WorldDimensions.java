package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.PrimaryLevelData;

public record WorldDimensions(Map<ResourceKey<LevelStem>, LevelStem> dimensions) {
   public static final MapCodec<WorldDimensions> CODEC = RecordCodecBuilder.mapCodec(
      p_327457_ -> p_327457_.group(
            Codec.unboundedMap(ResourceKey.codec(Registries.LEVEL_STEM), LevelStem.CODEC).fieldOf("dimensions").forGetter(WorldDimensions::dimensions)
         )
         .apply(p_327457_, p_327457_.stable(WorldDimensions::new))
   );
   private static final Set<ResourceKey<LevelStem>> BUILTIN_ORDER = ImmutableSet.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END);
   private static final int VANILLA_DIMENSION_COUNT = BUILTIN_ORDER.size();

   public WorldDimensions {
      LevelStem levelstem = dimensions.get(LevelStem.OVERWORLD);
      if (levelstem == null) {
         throw new IllegalStateException("Overworld settings missing");
      }
   }

   public WorldDimensions(Registry<LevelStem> p_251356_) {
      this(p_251356_.listElements().collect(Collectors.toMap(Holder.Reference::key, Holder.Reference::value)));
   }

   public static Stream<ResourceKey<LevelStem>> keysInOrder(Stream<ResourceKey<LevelStem>> p_251309_) {
      return Stream.concat(BUILTIN_ORDER.stream(), p_251309_.filter(p_251885_ -> !BUILTIN_ORDER.contains(p_251885_)));
   }

   public WorldDimensions replaceOverworldGenerator(HolderLookup.Provider p_363563_, ChunkGenerator p_248755_) {
      HolderLookup<DimensionType> holderlookup = p_363563_.lookupOrThrow(Registries.DIMENSION_TYPE);
      Map<ResourceKey<LevelStem>, LevelStem> map = withOverworld(holderlookup, this.dimensions, p_248755_);
      return new WorldDimensions(map);
   }

   public static Map<ResourceKey<LevelStem>, LevelStem> withOverworld(
      HolderLookup<DimensionType> p_362831_, Map<ResourceKey<LevelStem>, LevelStem> p_327923_, ChunkGenerator p_251737_
   ) {
      LevelStem levelstem = p_327923_.get(LevelStem.OVERWORLD);
      Holder<DimensionType> holder = levelstem == null ? p_362831_.getOrThrow(BuiltinDimensionTypes.OVERWORLD) : levelstem.type();
      return withOverworld(p_327923_, holder, p_251737_);
   }

   public static Map<ResourceKey<LevelStem>, LevelStem> withOverworld(
      Map<ResourceKey<LevelStem>, LevelStem> p_329337_, Holder<DimensionType> p_251895_, ChunkGenerator p_250220_
   ) {
      Builder<ResourceKey<LevelStem>, LevelStem> builder = ImmutableMap.builder();
      builder.putAll(p_329337_);
      builder.put(LevelStem.OVERWORLD, new LevelStem(p_251895_, p_250220_));
      return builder.buildKeepingLast();
   }

   public ChunkGenerator overworld() {
      LevelStem levelstem = this.dimensions.get(LevelStem.OVERWORLD);
      if (levelstem == null) {
         throw new IllegalStateException("Overworld settings missing");
      } else {
         return levelstem.generator();
      }
   }

   public Optional<LevelStem> get(ResourceKey<LevelStem> p_250824_) {
      return Optional.ofNullable(this.dimensions.get(p_250824_));
   }

   public ImmutableSet<ResourceKey<Level>> levels() {
      return this.dimensions().keySet().stream().map(Registries::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
   }

   public boolean isDebug() {
      return this.overworld() instanceof DebugLevelSource;
   }

   private static PrimaryLevelData.SpecialWorldProperty specialWorldProperty(Registry<LevelStem> p_251549_) {
      return p_251549_.getOptional(LevelStem.OVERWORLD).map(p_251481_ -> {
         ChunkGenerator chunkgenerator = p_251481_.generator();
         if (chunkgenerator instanceof DebugLevelSource) {
            return PrimaryLevelData.SpecialWorldProperty.DEBUG;
         } else {
            return chunkgenerator instanceof FlatLevelSource ? PrimaryLevelData.SpecialWorldProperty.FLAT : PrimaryLevelData.SpecialWorldProperty.NONE;
         }
      }).orElse(PrimaryLevelData.SpecialWorldProperty.NONE);
   }

   static Lifecycle checkStability(ResourceKey<LevelStem> p_250764_, LevelStem p_248865_) {
      return isVanillaLike(p_250764_, p_248865_) ? Lifecycle.stable() : Lifecycle.experimental();
   }

   private static boolean isVanillaLike(ResourceKey<LevelStem> p_250556_, LevelStem p_250034_) {
      if (p_250556_ == LevelStem.OVERWORLD) {
         return isStableOverworld(p_250034_);
      } else if (p_250556_ == LevelStem.NETHER) {
         return isStableNether(p_250034_);
      } else {
         return p_250556_ == LevelStem.END ? isStableEnd(p_250034_) : false;
      }
   }

   private static boolean isStableOverworld(LevelStem p_250762_) {
      Holder<DimensionType> holder = p_250762_.type();
      return !holder.is(BuiltinDimensionTypes.OVERWORLD) && !holder.is(BuiltinDimensionTypes.OVERWORLD_CAVES)
         ? false
         : !(
            p_250762_.generator().getBiomeSource() instanceof MultiNoiseBiomeSource multinoisebiomesource
               && !multinoisebiomesource.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
         );
   }

   private static boolean isStableNether(LevelStem p_250497_) {
      return p_250497_.type().is(BuiltinDimensionTypes.NETHER)
         && p_250497_.generator() instanceof NoiseBasedChunkGenerator noisebasedchunkgenerator
         && noisebasedchunkgenerator.stable(NoiseGeneratorSettings.NETHER)
         && noisebasedchunkgenerator.getBiomeSource() instanceof MultiNoiseBiomeSource multinoisebiomesource
         && multinoisebiomesource.stable(MultiNoiseBiomeSourceParameterLists.NETHER);
   }

   private static boolean isStableEnd(LevelStem p_250720_) {
      return p_250720_.type().is(BuiltinDimensionTypes.END)
         && p_250720_.generator() instanceof NoiseBasedChunkGenerator noisebasedchunkgenerator
         && noisebasedchunkgenerator.stable(NoiseGeneratorSettings.END)
         && noisebasedchunkgenerator.getBiomeSource() instanceof TheEndBiomeSource;
   }

   public WorldDimensions.Complete bake(Registry<LevelStem> p_248787_) {
      Stream<ResourceKey<LevelStem>> stream = Stream.concat(p_248787_.registryKeySet().stream(), this.dimensions.keySet().stream()).distinct();

      record Entry(ResourceKey<LevelStem> key, LevelStem value) {
         RegistrationInfo registrationInfo() {
            return new RegistrationInfo(Optional.empty(), WorldDimensions.checkStability(this.key, this.value));
         }
      }

      List<Entry> list = new ArrayList<>();
      keysInOrder(stream)
         .forEach(
            p_248571_ -> p_248787_.getOptional((ResourceKey<LevelStem>)p_248571_)
               .or(() -> Optional.ofNullable(this.dimensions.get(p_248571_)))
               .ifPresent(p_250263_ -> list.add(new Entry(p_248571_, p_250263_)))
         );
      Lifecycle lifecycle = list.size() == VANILLA_DIMENSION_COUNT ? Lifecycle.stable() : Lifecycle.experimental();
      WritableRegistry<LevelStem> writableregistry = new MappedRegistry<>(Registries.LEVEL_STEM, lifecycle);
      list.forEach(p_327459_ -> writableregistry.register(p_327459_.key, p_327459_.value, p_327459_.registrationInfo()));
      Registry<LevelStem> registry = writableregistry.freeze();
      PrimaryLevelData.SpecialWorldProperty primaryleveldata$specialworldproperty = specialWorldProperty(registry);
      return new WorldDimensions.Complete(registry.freeze(), primaryleveldata$specialworldproperty);
   }

   public record Complete(Registry<LevelStem> dimensions, PrimaryLevelData.SpecialWorldProperty specialWorldProperty) {
      public Lifecycle lifecycle() {
         return this.dimensions.registryLifecycle();
      }

      public RegistryAccess.Frozen dimensionsRegistryAccess() {
         return new RegistryAccess.ImmutableRegistryAccess(List.of(this.dimensions)).freeze();
      }
   }
}
