package net.minecraft.world.level;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public interface LevelReader extends BlockAndLightGetter, CollisionGetter, SignalGetter, BiomeManager.NoiseBiomeSource {
   @Nullable ChunkAccess getChunk(final int chunkX, final int chunkZ, final ChunkStatus targetStatus, final boolean loadOrGenerate);

   @Deprecated
   boolean hasChunk(int chunkX, int chunkZ);

   int getHeight(Heightmap.Types type, int x, int z);

   default int getHeight(final Heightmap.Types type, final BlockPos pos) {
      return this.getHeight(type, pos.getX(), pos.getZ());
   }

   int getSkyDarken();

   BiomeManager getBiomeManager();

   default Holder<Biome> getBiome(final BlockPos pos) {
      return this.getBiomeManager().getBiome(pos);
   }

   default Stream<BlockState> getBlockStatesIfLoaded(final AABB box) {
      int x0 = Mth.floor(box.minX);
      int x1 = Mth.floor(box.maxX);
      int y0 = Mth.floor(box.minY);
      int y1 = Mth.floor(box.maxY);
      int z0 = Mth.floor(box.minZ);
      int z1 = Mth.floor(box.maxZ);
      return this.hasChunksAt(x0, y0, z0, x1, y1, z1) ? this.getBlockStates(box) : Stream.empty();
   }

   @Override
   default Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ) {
      ChunkAccess chunk = this.getChunk(QuartPos.toSection(quartX), QuartPos.toSection(quartZ), ChunkStatus.BIOMES, false);
      return chunk != null ? chunk.getNoiseBiome(quartX, quartY, quartZ) : this.getUncachedNoiseBiome(quartX, quartY, quartZ);
   }

   Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ);

   boolean isClientSide();

   int getSeaLevel();

   DimensionType dimensionType();

   @Override
   default int getMinY() {
      return this.dimensionType().minY();
   }

   @Override
   default int getHeight() {
      return this.dimensionType().height();
   }

   default BlockPos getHeightmapPos(final Heightmap.Types type, final BlockPos pos) {
      return new BlockPos(pos.getX(), this.getHeight(type, pos.getX(), pos.getZ()), pos.getZ());
   }

   default boolean isEmptyBlock(final BlockPos pos) {
      return this.getBlockState(pos).isAir();
   }

   default boolean canSeeSkyFromBelowWater(final BlockPos pos) {
      if (pos.getY() >= this.getSeaLevel()) {
         return this.canSeeSky(pos);
      }

      BlockPos scanPoint = new BlockPos(pos.getX(), this.getSeaLevel(), pos.getZ());
      if (!this.canSeeSky(scanPoint)) {
         return false;
      }

      for (BlockPos var4 = scanPoint.below(); var4.getY() > pos.getY(); var4 = var4.below()) {
         BlockState state = this.getBlockState(var4);
         if (state.getLightDampening() > 0 && !state.liquid()) {
            return false;
         }
      }

      return true;
   }

   default float getPathfindingCostFromLightLevels(final BlockPos pos) {
      return this.getLightLevelDependentMagicValue(pos) - 0.5F;
   }

   @Deprecated
   default float getLightLevelDependentMagicValue(final BlockPos pos) {
      float v = this.getMaxLocalRawBrightness(pos) / 15.0F;
      float curvedV = v / (4.0F - 3.0F * v);
      return Mth.lerp(this.dimensionType().ambientLight(), curvedV, 1.0F);
   }

   default ChunkAccess getChunk(final BlockPos pos) {
      return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
   }

   default ChunkAccess getChunk(final int chunkX, final int chunkZ) {
      return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
   }

   default ChunkAccess getChunk(final int chunkX, final int chunkZ, final ChunkStatus status) {
      return this.getChunk(chunkX, chunkZ, status, true);
   }

   @Override
   default @Nullable BlockGetter getChunkForCollisions(final int chunkX, final int chunkZ) {
      return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
   }

   default boolean isWaterAt(final BlockPos pos) {
      return this.getFluidState(pos).is(FluidTags.WATER);
   }

   default boolean containsAnyLiquid(final AABB box) {
      return this.findBlocksIn(box).filterState(state -> !state.getFluidState().isEmpty()).anyMatched();
   }

   default int getMaxLocalRawBrightness(final BlockPos pos) {
      return this.getMaxLocalRawBrightness(pos, this.getSkyDarken());
   }

   default int getMaxLocalRawBrightness(final BlockPos pos, final int skyDarkening) {
      return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000
         ? this.getRawBrightness(pos, skyDarkening)
         : 15;
   }

   default int getEffectiveSkyBrightness(final BlockPos pos) {
      return this.getBrightness(LightLayer.SKY, pos) - this.getSkyDarken();
   }

   default BlockScanUtils.BlockMatcher findBlocksIn(final AABB box) {
      return new BlockScanUtils.BlockMatcher(this, box);
   }

   default BlockScanUtils.BlockMatcher findBlocksIn(final BlockPos from, final BlockPos to) {
      return new BlockScanUtils.BlockMatcher(this, from, to);
   }

   @Deprecated
   default boolean hasChunkAt(final int blockX, final int blockZ) {
      return this.hasChunk(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
   }

   @Deprecated
   default boolean hasChunkAt(final BlockPos pos) {
      return this.hasChunkAt(pos.getX(), pos.getZ());
   }

   @Deprecated
   default boolean hasChunksAt(final BlockPos pos0, final BlockPos pos1) {
      return this.hasChunksAt(pos0.getX(), pos0.getY(), pos0.getZ(), pos1.getX(), pos1.getY(), pos1.getZ());
   }

   @Deprecated
   default boolean hasChunksAt(final int x0, final int y0, final int z0, final int x1, final int y1, final int z1) {
      return y1 >= this.getMinY() && y0 <= this.getMaxY() ? this.hasChunksAt(x0, z0, x1, z1) : false;
   }

   @Deprecated
   default boolean hasChunksAt(final int x0, final int z0, final int x1, final int z1) {
      int chunkX0 = SectionPos.blockToSectionCoord(x0);
      int chunkX1 = SectionPos.blockToSectionCoord(x1);
      int chunkZ0 = SectionPos.blockToSectionCoord(z0);
      int chunkZ1 = SectionPos.blockToSectionCoord(z1);

      for (int chunkX = chunkX0; chunkX <= chunkX1; chunkX++) {
         for (int chunkZ = chunkZ0; chunkZ <= chunkZ1; chunkZ++) {
            if (!this.hasChunk(chunkX, chunkZ)) {
               return false;
            }
         }
      }

      return true;
   }

   RegistryAccess registryAccess();

   FeatureFlagSet enabledFeatures();

   default <T> HolderLookup<T> holderLookup(final ResourceKey<? extends Registry<? extends T>> key) {
      Registry<T> registry = this.registryAccess().lookupOrThrow(key);
      return registry.filterFeatures(this.enabledFeatures());
   }

   EnvironmentAttributeReader environmentAttributes();
}
