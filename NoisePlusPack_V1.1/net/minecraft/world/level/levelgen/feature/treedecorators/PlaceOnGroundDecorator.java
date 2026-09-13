package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class PlaceOnGroundDecorator extends TreeDecorator {
   public static final MapCodec<PlaceOnGroundDecorator> CODEC = RecordCodecBuilder.mapCodec(
      p_395435_ -> p_395435_.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("tries").orElse(128).forGetter(p_395254_ -> p_395254_.tries),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("radius").orElse(2).forGetter(p_395089_ -> p_395089_.radius),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("height").orElse(1).forGetter(p_392656_ -> p_392656_.height),
            BlockStateProvider.CODEC.fieldOf("block_state_provider").forGetter(p_397450_ -> p_397450_.blockStateProvider)
         )
         .apply(p_395435_, PlaceOnGroundDecorator::new)
   );
   private final int tries;
   private final int radius;
   private final int height;
   private final BlockStateProvider blockStateProvider;

   public PlaceOnGroundDecorator(int p_391216_, int p_393843_, int p_397591_, BlockStateProvider p_391209_) {
      this.tries = p_391216_;
      this.radius = p_393843_;
      this.height = p_397591_;
      this.blockStateProvider = p_391209_;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.PLACE_ON_GROUND;
   }

   @Override
   public void place(TreeDecorator.Context p_395335_) {
      List<BlockPos> list = TreeFeature.getLowestTrunkOrRootOfTree(p_395335_);
      if (!list.isEmpty()) {
         BlockPos blockpos = list.getFirst();
         int i = blockpos.getY();
         int j = blockpos.getX();
         int k = blockpos.getX();
         int l = blockpos.getZ();
         int i1 = blockpos.getZ();

         for (BlockPos blockpos1 : list) {
            if (blockpos1.getY() == i) {
               j = Math.min(j, blockpos1.getX());
               k = Math.max(k, blockpos1.getX());
               l = Math.min(l, blockpos1.getZ());
               i1 = Math.max(i1, blockpos1.getZ());
            }
         }

         RandomSource randomsource = p_395335_.random();
         BoundingBox boundingbox = new BoundingBox(j, i, l, k, i, i1).inflatedBy(this.radius, this.height, this.radius);
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for (int j1 = 0; j1 < this.tries; j1++) {
            blockpos$mutableblockpos.set(
               randomsource.nextIntBetweenInclusive(boundingbox.minX(), boundingbox.maxX()),
               randomsource.nextIntBetweenInclusive(boundingbox.minY(), boundingbox.maxY()),
               randomsource.nextIntBetweenInclusive(boundingbox.minZ(), boundingbox.maxZ())
            );
            this.attemptToPlaceBlockAbove(p_395335_, blockpos$mutableblockpos);
         }
      }
   }

   private void attemptToPlaceBlockAbove(TreeDecorator.Context p_393460_, BlockPos p_391491_) {
      BlockPos blockpos = p_391491_.above();
      if (p_393460_.level().isStateAtPosition(blockpos, p_395132_ -> p_395132_.isAir() || p_395132_.is(Blocks.VINE))
         && p_393460_.checkBlock(p_391491_, BlockBehaviour.BlockStateBase::isSolidRender)
         && p_393460_.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p_391491_).getY() <= blockpos.getY()) {
         p_393460_.setBlock(blockpos, this.blockStateProvider.getState(p_393460_.random(), blockpos));
      }
   }
}
