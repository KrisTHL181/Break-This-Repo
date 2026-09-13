package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public class HeightmapPlacement extends PlacementModifier {
   public static final MapCodec<HeightmapPlacement> CODEC = RecordCodecBuilder.mapCodec(
      p_191701_ -> p_191701_.group(Heightmap.Types.CODEC.fieldOf("heightmap").forGetter(p_191705_ -> p_191705_.heightmap))
         .apply(p_191701_, HeightmapPlacement::new)
   );
   private final Heightmap.Types heightmap;

   private HeightmapPlacement(Heightmap.Types p_191699_) {
      this.heightmap = p_191699_;
   }

   public static HeightmapPlacement onHeightmap(Heightmap.Types p_191703_) {
      return new HeightmapPlacement(p_191703_);
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext p_226344_, RandomSource p_226345_, BlockPos p_226346_) {
      int i = p_226346_.getX();
      int j = p_226346_.getZ();
      int k = p_226344_.getHeight(this.heightmap, i, j);
      return k > p_226344_.getMinY() ? Stream.of(new BlockPos(i, k, j)) : Stream.of();
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.HEIGHTMAP;
   }
}
