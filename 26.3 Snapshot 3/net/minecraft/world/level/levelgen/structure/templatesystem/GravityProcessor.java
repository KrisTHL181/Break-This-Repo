package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public class GravityProcessor implements StructureProcessor {
   public static final MapCodec<GravityProcessor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Heightmap.Types.CODEC.optionalFieldOf("heightmap", Heightmap.Types.WORLD_SURFACE_WG).forGetter(p -> p.heightmap),
            Codec.INT.optionalFieldOf("offset", 0).forGetter(p -> p.offset)
         )
         .apply(i, GravityProcessor::new)
   );
   private final Heightmap.Types heightmap;
   private final int offset;

   public GravityProcessor(final Heightmap.Types heightmap, final int offset) {
      this.heightmap = heightmap;
      this.offset = offset;
   }

   @Override
   public StructureTemplate.@Nullable StructureBlockInfo processBlock(
      final LevelReader level,
      final BlockPos targetPosition,
      final BlockPos referencePos,
      final BlockPos templateRelativePos,
      final StructureTemplate.StructureBlockInfo processedBlockInfo,
      final StructurePlaceSettings settings
   ) {
      Heightmap.Types heightmap;
      if (level instanceof ServerLevel) {
         if (this.heightmap == Heightmap.Types.WORLD_SURFACE_WG) {
            heightmap = Heightmap.Types.WORLD_SURFACE;
         } else if (this.heightmap == Heightmap.Types.OCEAN_FLOOR_WG) {
            heightmap = Heightmap.Types.OCEAN_FLOOR;
         } else {
            heightmap = this.heightmap;
         }
      } else {
         heightmap = this.heightmap;
      }

      BlockPos pos = processedBlockInfo.pos();
      int height = level.getHeight(heightmap, pos.getX(), pos.getZ()) + this.offset;
      int delta = templateRelativePos.getY();
      return new StructureTemplate.StructureBlockInfo(
         new BlockPos(pos.getX(), height + delta, pos.getZ()), processedBlockInfo.state(), processedBlockInfo.nbt()
      );
   }

   @Override
   public MapCodec<GravityProcessor> codec() {
      return MAP_CODEC;
   }
}
