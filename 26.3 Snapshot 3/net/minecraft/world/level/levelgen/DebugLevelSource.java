package net.minecraft.world.level.levelgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.jspecify.annotations.Nullable;

public class DebugLevelSource extends ChunkGenerator {
   public static final MapCodec<DebugLevelSource> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(RegistryOps.retrieveElement(Biomes.PLAINS)).apply(i, i.stable(DebugLevelSource::new))
   );
   private static final int BLOCK_MARGIN = 2;
   private static final List<BlockState> ALL_BLOCKS = StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false)
      .flatMap(b -> b.getStateDefinition().getPossibleStates().stream())
      .collect(Collectors.toList());
   private static final int GRID_WIDTH = Mth.ceil(Mth.sqrt(ALL_BLOCKS.size()));
   private static final int GRID_HEIGHT = Mth.ceil((float)ALL_BLOCKS.size() / GRID_WIDTH);
   protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
   protected static final BlockState BARRIER = Blocks.BARRIER.defaultBlockState();
   public static final int HEIGHT = 70;
   public static final int BARRIER_HEIGHT = 60;

   public DebugLevelSource(final Holder.Reference<Biome> plains) {
      super(new FixedBiomeSource(plains));
   }

   @Override
   protected MapCodec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   @Override
   public void buildSurface(final WorldGenRegion level, final StructureManager structureManager, final RandomState randomState, final ChunkAccess protoChunk) {
   }

   @Override
   public void applyBiomeDecoration(final WorldGenLevel level, final ChunkAccess chunk, final StructureManager structureManager) {
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
      ChunkPos centerPos = chunk.getPos();
      int chunkX = centerPos.x();
      int chunkZ = centerPos.z();

      for (int x = 0; x < 16; x++) {
         for (int z = 0; z < 16; z++) {
            int worldX = SectionPos.sectionToBlockCoord(chunkX, x);
            int worldZ = SectionPos.sectionToBlockCoord(chunkZ, z);
            level.setBlock(blockPos.set(worldX, 60, worldZ), BARRIER, 2);
            BlockState state = getBlockStateFor(worldX, worldZ);
            level.setBlock(blockPos.set(worldX, 70, worldZ), state, 2);
         }
      }
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(
      final Blender blender, final RandomState randomState, final StructureManager structureManager, final ChunkAccess centerChunk
   ) {
      return CompletableFuture.completedFuture(centerChunk);
   }

   @Override
   public int getBaseHeight(final int x, final int z, final Heightmap.Types type, final LevelHeightAccessor heightAccessor, final RandomState randomState) {
      return 0;
   }

   @Override
   public NoiseColumn getBaseColumn(final int x, final int z, final LevelHeightAccessor heightAccessor, final RandomState randomState) {
      return new NoiseColumn(0, new BlockState[0]);
   }

   @Override
   public void addDebugScreenInfo(final List<String> result, final RandomState randomState, final BlockPos feetPos) {
   }

   public static BlockState getBlockStateFor(int worldX, int worldZ) {
      BlockState state = AIR;
      if (worldX > 0 && worldZ > 0 && worldX % 2 != 0 && worldZ % 2 != 0) {
         worldX /= 2;
         worldZ /= 2;
         if (worldX <= GRID_WIDTH && worldZ <= GRID_HEIGHT) {
            int index = Mth.abs(worldX * GRID_WIDTH + worldZ);
            if (index < ALL_BLOCKS.size()) {
               state = ALL_BLOCKS.get(index);
            }
         }
      }

      return state;
   }

   @Override
   public void applyCarvers(
      final WorldGenRegion region,
      final long seed,
      final RandomState randomState,
      final BiomeManager biomeManager,
      final StructureManager structureManager,
      final ChunkAccess chunk,
      final CarvingMask.@Nullable Filter filter
   ) {
   }

   @Override
   public void spawnOriginalMobs(final WorldGenRegion worldGenRegion) {
   }

   @Override
   public int getMinY() {
      return 0;
   }

   @Override
   public int getGenDepth() {
      return 384;
   }

   @Override
   public int getSeaLevel() {
      return 63;
   }
}
