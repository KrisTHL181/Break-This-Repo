package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public record HeightmapPlacement(Heightmap.Types heightmap) implements PlacementModifier {
   public static final MapCodec<HeightmapPlacement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Heightmap.Types.CODEC.fieldOf("heightmap").forGetter(HeightmapPlacement::heightmap)).apply(i, HeightmapPlacement::new)
   );

   public static HeightmapPlacement onHeightmap(final Heightmap.Types heightmap) {
      return new HeightmapPlacement(heightmap);
   }

   @Override
   public void modify(final PlacementContext context, final RandomSource random, final BlockPos origin, final Consumer<BlockPos> output) {
      int x = origin.getX();
      int z = origin.getZ();
      int height = context.getHeight(this.heightmap, x, z);
      if (height > context.getMinY()) {
         output.accept(new BlockPos(x, height, z));
      }
   }

   @Override
   public MapCodec<HeightmapPlacement> codec() {
      return CODEC;
   }
}
