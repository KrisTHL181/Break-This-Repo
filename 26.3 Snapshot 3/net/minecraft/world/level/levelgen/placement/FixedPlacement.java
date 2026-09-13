package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;

public record FixedPlacement(List<BlockPos> positions) implements PlacementModifier {
   public static final MapCodec<FixedPlacement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(BlockPos.CODEC.listOf().fieldOf("positions").forGetter(c -> c.positions)).apply(i, FixedPlacement::new)
   );

   public static FixedPlacement of(final BlockPos... pos) {
      return new FixedPlacement(List.of(pos));
   }

   @Override
   public void modify(final PlacementContext context, final RandomSource random, final BlockPos origin, final Consumer<BlockPos> output) {
      int chunkX = SectionPos.blockToSectionCoord(origin.getX());
      int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());

      for (BlockPos position : this.positions) {
         if (isSameChunk(chunkX, chunkZ, position)) {
            output.accept(position);
         }
      }
   }

   private static boolean isSameChunk(final int chunkX, final int chunkZ, final BlockPos position) {
      return chunkX == SectionPos.blockToSectionCoord(position.getX()) && chunkZ == SectionPos.blockToSectionCoord(position.getZ());
   }

   @Override
   public MapCodec<FixedPlacement> codec() {
      return CODEC;
   }
}
