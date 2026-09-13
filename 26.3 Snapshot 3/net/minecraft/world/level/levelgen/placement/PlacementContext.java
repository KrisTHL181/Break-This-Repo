package net.minecraft.world.level.levelgen.placement;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class PlacementContext extends WorldGenerationContext {
   private final WorldGenLevel level;
   private final ChunkGenerator generator;
   private final Optional<PlacedFeature> topFeature;

   public PlacementContext(final WorldGenLevel level, final ChunkGenerator generator, final Optional<PlacedFeature> topFeature) {
      super(generator, level);
      this.level = level;
      this.generator = generator;
      this.topFeature = topFeature;
   }

   public int getHeight(final Heightmap.Types type, final int x, final int z) {
      return this.level.getHeight(type, x, z);
   }

   public BlockState getBlockState(final BlockPos pos) {
      return this.level.getBlockState(pos);
   }

   public int getMinY() {
      return this.level.getMinY();
   }

   public WorldGenLevel getLevel() {
      return this.level;
   }

   public Optional<PlacedFeature> topFeature() {
      return this.topFeature;
   }

   public ChunkGenerator generator() {
      return this.generator;
   }
}
