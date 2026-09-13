package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

public record BambooFeature(float probability) implements Feature {
   private static final BlockState BAMBOO_TRUNK = Blocks.BAMBOO
      .defaultBlockState()
      .setValue(BambooStalkBlock.AGE, 1)
      .setValue(BambooStalkBlock.LEAVES, BambooLeaves.NONE)
      .setValue(BambooStalkBlock.STAGE, 0);
   private static final BlockState BAMBOO_FINAL_LARGE = BAMBOO_TRUNK.setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE).setValue(BambooStalkBlock.STAGE, 1);
   private static final BlockState BAMBOO_TOP_LARGE = BAMBOO_TRUNK.setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE);
   private static final BlockState BAMBOO_TOP_SMALL = BAMBOO_TRUNK.setValue(BambooStalkBlock.LEAVES, BambooLeaves.SMALL);
   public static final MapCodec<BambooFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(BambooFeature::probability)).apply(i, BambooFeature::new)
   );

   @Override
   public MapCodec<BambooFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      int placed = 0;
      BlockPos.MutableBlockPos bambooPos = origin.mutable();
      BlockPos.MutableBlockPos podzolPos = origin.mutable();
      if (level.isEmptyBlock(bambooPos)) {
         if (Blocks.BAMBOO.defaultBlockState().canSurvive(level, bambooPos)) {
            int height = random.nextInt(12) + 5;
            if (random.nextFloat() < this.probability) {
               int r = random.nextInt(4) + 1;

               for (int xx = origin.getX() - r; xx <= origin.getX() + r; xx++) {
                  for (int zz = origin.getZ() - r; zz <= origin.getZ() + r; zz++) {
                     int xd = xx - origin.getX();
                     int zd = zz - origin.getZ();
                     if (xd * xd + zd * zd <= r * r) {
                        podzolPos.set(xx, level.getHeight(Heightmap.Types.WORLD_SURFACE, xx, zz) - 1, zz);
                        if (level.getBlockState(podzolPos).is(BlockTags.BENEATH_BAMBOO_PODZOL_REPLACEABLE)) {
                           level.setBlock(podzolPos, Blocks.PODZOL.defaultBlockState(), 2);
                        }
                     }
                  }
               }
            }

            for (int i = 0; i < height && level.isEmptyBlock(bambooPos); i++) {
               level.setBlock(bambooPos, BAMBOO_TRUNK, 2);
               bambooPos.move(Direction.UP, 1);
            }

            if (bambooPos.getY() - origin.getY() >= 3) {
               level.setBlock(bambooPos, BAMBOO_FINAL_LARGE, 2);
               level.setBlock(bambooPos.move(Direction.DOWN, 1), BAMBOO_TOP_LARGE, 2);
               level.setBlock(bambooPos.move(Direction.DOWN, 1), BAMBOO_TOP_SMALL, 2);
            }
         }

         placed++;
      }

      return placed > 0;
   }
}
