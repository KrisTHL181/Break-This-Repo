package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record EndGatewayFeature(Optional<BlockPos> exit, boolean exact) implements Feature {
   public static final MapCodec<EndGatewayFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(BlockPos.CODEC.optionalFieldOf("exit").forGetter(EndGatewayFeature::exit), Codec.BOOL.fieldOf("exact").forGetter(EndGatewayFeature::exact))
         .apply(i, EndGatewayFeature::new)
   );

   @Override
   public MapCodec<EndGatewayFeature> codec() {
      return CODEC;
   }

   public static EndGatewayFeature knownExit(final BlockPos exit, final boolean exact) {
      return new EndGatewayFeature(Optional.of(exit), exact);
   }

   public static EndGatewayFeature delayedExitSearch() {
      return new EndGatewayFeature(Optional.empty(), false);
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -2, -1), origin.offset(1, 2, 1))) {
         boolean sameX = pos.getX() == origin.getX();
         boolean sameY = pos.getY() == origin.getY();
         boolean sameZ = pos.getZ() == origin.getZ();
         boolean end = Math.abs(pos.getY() - origin.getY()) == 2;
         if (sameX && sameY && sameZ) {
            BlockPos immutable = pos.immutable();
            this.setBlock(level, immutable, Blocks.END_GATEWAY.defaultBlockState());
            this.exit.ifPresent(targetPos -> {
               if (level.getBlockEntity(immutable) instanceof TheEndGatewayBlockEntity exitGateway) {
                  exitGateway.setExitPosition(targetPos, this.exact);
               }
            });
         } else if (sameY) {
            this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
         } else if (end && sameX && sameZ) {
            this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
         } else if ((sameX || sameZ) && !end) {
            this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
         } else {
            this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
         }
      }

      return true;
   }
}
