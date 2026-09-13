package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public abstract class Structure {
   public static final Codec<Structure> DIRECT_CODEC = BuiltInRegistries.STRUCTURE_TYPE.byNameCodec().dispatch(Structure::type, StructureType::codec);
   public static final Codec<Holder<Structure>> CODEC = RegistryFileCodec.create(Registries.STRUCTURE, DIRECT_CODEC);
   protected final Structure.StructureSettings settings;

   public static <S extends Structure> RecordCodecBuilder<S, Structure.StructureSettings> settingsCodec(Instance<S> p_226568_) {
      return Structure.StructureSettings.CODEC.forGetter(p_226595_ -> p_226595_.settings);
   }

   public static <S extends Structure> MapCodec<S> simpleCodec(Function<Structure.StructureSettings, S> p_226608_) {
      return RecordCodecBuilder.mapCodec(p_226611_ -> p_226611_.group(settingsCodec(p_226611_)).apply(p_226611_, p_226608_));
   }

   protected Structure(Structure.StructureSettings p_226558_) {
      this.settings = p_226558_;
   }

   public HolderSet<Biome> biomes() {
      return this.settings.biomes;
   }

   public Map<MobCategory, StructureSpawnOverride> spawnOverrides() {
      return this.settings.spawnOverrides;
   }

   public GenerationStep.Decoration step() {
      return this.settings.step;
   }

   public TerrainAdjustment terrainAdaptation() {
      return this.settings.terrainAdaptation;
   }

   public BoundingBox adjustBoundingBox(BoundingBox p_226570_) {
      return this.terrainAdaptation() != TerrainAdjustment.NONE ? p_226570_.inflatedBy(12) : p_226570_;
   }

   public StructureStart generate(
      Holder<Structure> p_378494_,
      ResourceKey<Level> p_376569_,
      RegistryAccess p_226597_,
      ChunkGenerator p_226598_,
      BiomeSource p_226599_,
      RandomState p_226600_,
      StructureTemplateManager p_226601_,
      long p_226602_,
      ChunkPos p_226603_,
      int p_226604_,
      LevelHeightAccessor p_226605_,
      Predicate<Holder<Biome>> p_226606_
   ) {
      ProfiledDuration profiledduration = JvmProfiler.INSTANCE.onStructureGenerate(p_226603_, p_376569_, p_378494_);
      Structure.GenerationContext structure$generationcontext = new Structure.GenerationContext(
         p_226597_, p_226598_, p_226599_, p_226600_, p_226601_, p_226602_, p_226603_, p_226605_, p_226606_
      );
      Optional<Structure.GenerationStub> optional = this.findValidGenerationPoint(structure$generationcontext);
      if (optional.isPresent()) {
         StructurePiecesBuilder structurepiecesbuilder = optional.get().getPiecesBuilder();
         StructureStart structurestart = new StructureStart(this, p_226603_, p_226604_, structurepiecesbuilder.build());
         if (structurestart.isValid()) {
            if (profiledduration != null) {
               profiledduration.finish(true);
            }

            return structurestart;
         }
      }

      if (profiledduration != null) {
         profiledduration.finish(false);
      }

      return StructureStart.INVALID_START;
   }

   protected static Optional<Structure.GenerationStub> onTopOfChunkCenter(
      Structure.GenerationContext p_226586_, Heightmap.Types p_226587_, Consumer<StructurePiecesBuilder> p_226588_
   ) {
      ChunkPos chunkpos = p_226586_.chunkPos();
      int i = chunkpos.getMiddleBlockX();
      int j = chunkpos.getMiddleBlockZ();
      int k = p_226586_.chunkGenerator().getFirstOccupiedHeight(i, j, p_226587_, p_226586_.heightAccessor(), p_226586_.randomState());
      return Optional.of(new Structure.GenerationStub(new BlockPos(i, k, j), p_226588_));
   }

   private static boolean isValidBiome(Structure.GenerationStub p_263042_, Structure.GenerationContext p_263005_) {
      BlockPos blockpos = p_263042_.position();
      return p_263005_.validBiome
         .test(
            p_263005_.chunkGenerator
               .getBiomeSource()
               .getNoiseBiome(
                  QuartPos.fromBlock(blockpos.getX()),
                  QuartPos.fromBlock(blockpos.getY()),
                  QuartPos.fromBlock(blockpos.getZ()),
                  p_263005_.randomState.sampler()
               )
         );
   }

   public void afterPlace(
      WorldGenLevel p_226560_,
      StructureManager p_226561_,
      ChunkGenerator p_226562_,
      RandomSource p_226563_,
      BoundingBox p_226564_,
      ChunkPos p_226565_,
      PiecesContainer p_226566_
   ) {
   }

   private static int[] getCornerHeights(Structure.GenerationContext p_226614_, int p_226615_, int p_226616_, int p_226617_, int p_226618_) {
      ChunkGenerator chunkgenerator = p_226614_.chunkGenerator();
      LevelHeightAccessor levelheightaccessor = p_226614_.heightAccessor();
      RandomState randomstate = p_226614_.randomState();
      return new int[]{
         chunkgenerator.getFirstOccupiedHeight(p_226615_, p_226617_, Heightmap.Types.WORLD_SURFACE_WG, levelheightaccessor, randomstate),
         chunkgenerator.getFirstOccupiedHeight(p_226615_, p_226617_ + p_226618_, Heightmap.Types.WORLD_SURFACE_WG, levelheightaccessor, randomstate),
         chunkgenerator.getFirstOccupiedHeight(p_226615_ + p_226616_, p_226617_, Heightmap.Types.WORLD_SURFACE_WG, levelheightaccessor, randomstate),
         chunkgenerator.getFirstOccupiedHeight(p_226615_ + p_226616_, p_226617_ + p_226618_, Heightmap.Types.WORLD_SURFACE_WG, levelheightaccessor, randomstate)
      };
   }

   public static int getMeanFirstOccupiedHeight(Structure.GenerationContext p_334739_, int p_329786_, int p_332089_, int p_333818_, int p_333198_) {
      int[] aint = getCornerHeights(p_334739_, p_329786_, p_332089_, p_333818_, p_333198_);
      return (aint[0] + aint[1] + aint[2] + aint[3]) / 4;
   }

   protected static int getLowestY(Structure.GenerationContext p_226573_, int p_226574_, int p_226575_) {
      ChunkPos chunkpos = p_226573_.chunkPos();
      int i = chunkpos.getMinBlockX();
      int j = chunkpos.getMinBlockZ();
      return getLowestY(p_226573_, i, j, p_226574_, p_226575_);
   }

   protected static int getLowestY(Structure.GenerationContext p_226577_, int p_226578_, int p_226579_, int p_226580_, int p_226581_) {
      int[] aint = getCornerHeights(p_226577_, p_226578_, p_226580_, p_226579_, p_226581_);
      return Math.min(Math.min(aint[0], aint[1]), Math.min(aint[2], aint[3]));
   }

   @Deprecated
   protected BlockPos getLowestYIn5by5BoxOffset7Blocks(Structure.GenerationContext p_226583_, Rotation p_226584_) {
      int i = 5;
      int j = 5;
      if (p_226584_ == Rotation.CLOCKWISE_90) {
         i = -5;
      } else if (p_226584_ == Rotation.CLOCKWISE_180) {
         i = -5;
         j = -5;
      } else if (p_226584_ == Rotation.COUNTERCLOCKWISE_90) {
         j = -5;
      }

      ChunkPos chunkpos = p_226583_.chunkPos();
      int k = chunkpos.getBlockX(7);
      int l = chunkpos.getBlockZ(7);
      return new BlockPos(k, getLowestY(p_226583_, k, l, i, j), l);
   }

   protected abstract Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext var1);

   public Optional<Structure.GenerationStub> findValidGenerationPoint(Structure.GenerationContext p_263060_) {
      return this.findGenerationPoint(p_263060_).filter(p_262911_ -> isValidBiome(p_262911_, p_263060_));
   }

   public abstract StructureType<?> type();

   public record GenerationContext(
      RegistryAccess registryAccess,
      ChunkGenerator chunkGenerator,
      BiomeSource biomeSource,
      RandomState randomState,
      StructureTemplateManager structureTemplateManager,
      WorldgenRandom random,
      long seed,
      ChunkPos chunkPos,
      LevelHeightAccessor heightAccessor,
      Predicate<Holder<Biome>> validBiome
   ) {
      public GenerationContext(
         RegistryAccess p_226632_,
         ChunkGenerator p_226633_,
         BiomeSource p_226634_,
         RandomState p_226635_,
         StructureTemplateManager p_226636_,
         long p_226637_,
         ChunkPos p_226638_,
         LevelHeightAccessor p_226639_,
         Predicate<Holder<Biome>> p_226640_
      ) {
         this(p_226632_, p_226633_, p_226634_, p_226635_, p_226636_, makeRandom(p_226637_, p_226638_), p_226637_, p_226638_, p_226639_, p_226640_);
      }

      private static WorldgenRandom makeRandom(long p_226654_, ChunkPos p_226655_) {
         WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(0L));
         worldgenrandom.setLargeFeatureSeed(p_226654_, p_226655_.x, p_226655_.z);
         return worldgenrandom;
      }
   }

   public record GenerationStub(BlockPos position, Either<Consumer<StructurePiecesBuilder>, StructurePiecesBuilder> generator) {
      public GenerationStub(BlockPos p_226675_, Consumer<StructurePiecesBuilder> p_226676_) {
         this(p_226675_, Either.left(p_226676_));
      }

      public StructurePiecesBuilder getPiecesBuilder() {
         return (StructurePiecesBuilder)this.generator.map(p_226681_ -> {
            StructurePiecesBuilder structurepiecesbuilder = new StructurePiecesBuilder();
            p_226681_.accept(structurepiecesbuilder);
            return structurepiecesbuilder;
         }, p_226679_ -> p_226679_);
      }
   }

   public record StructureSettings(
      HolderSet<Biome> biomes, Map<MobCategory, StructureSpawnOverride> spawnOverrides, GenerationStep.Decoration step, TerrainAdjustment terrainAdaptation
   ) {
      static final Structure.StructureSettings DEFAULT = new Structure.StructureSettings(
         HolderSet.direct(), Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE
      );
      public static final MapCodec<Structure.StructureSettings> CODEC = RecordCodecBuilder.mapCodec(
         p_341910_ -> p_341910_.group(
               RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(Structure.StructureSettings::biomes),
               Codec.simpleMap(MobCategory.CODEC, StructureSpawnOverride.CODEC, StringRepresentable.keys(MobCategory.values()))
                  .fieldOf("spawn_overrides")
                  .forGetter(Structure.StructureSettings::spawnOverrides),
               GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(Structure.StructureSettings::step),
               TerrainAdjustment.CODEC
                  .optionalFieldOf("terrain_adaptation", DEFAULT.terrainAdaptation)
                  .forGetter(Structure.StructureSettings::terrainAdaptation)
            )
            .apply(p_341910_, Structure.StructureSettings::new)
      );

      public StructureSettings(HolderSet<Biome> p_345322_) {
         this(p_345322_, DEFAULT.spawnOverrides, DEFAULT.step, DEFAULT.terrainAdaptation);
      }

      public static class Builder {
         private final HolderSet<Biome> biomes;
         private Map<MobCategory, StructureSpawnOverride> spawnOverrides;
         private GenerationStep.Decoration step;
         private TerrainAdjustment terrainAdaption;

         public Builder(HolderSet<Biome> p_345430_) {
            this.spawnOverrides = Structure.StructureSettings.DEFAULT.spawnOverrides;
            this.step = Structure.StructureSettings.DEFAULT.step;
            this.terrainAdaption = Structure.StructureSettings.DEFAULT.terrainAdaptation;
            this.biomes = p_345430_;
         }

         public Structure.StructureSettings.Builder spawnOverrides(Map<MobCategory, StructureSpawnOverride> p_344031_) {
            this.spawnOverrides = p_344031_;
            return this;
         }

         public Structure.StructureSettings.Builder generationStep(GenerationStep.Decoration p_342290_) {
            this.step = p_342290_;
            return this;
         }

         public Structure.StructureSettings.Builder terrainAdapation(TerrainAdjustment p_344665_) {
            this.terrainAdaption = p_344665_;
            return this;
         }

         public Structure.StructureSettings build() {
            return new Structure.StructureSettings(this.biomes, this.spawnOverrides, this.step, this.terrainAdaption);
         }
      }
   }
}
