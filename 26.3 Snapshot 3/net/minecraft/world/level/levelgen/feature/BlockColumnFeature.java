package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record BlockColumnFeature(List<BlockColumnFeature.Layer> layers, Direction direction, BlockPredicate allowedPlacement, boolean prioritizeTip)
   implements Feature {
   public static final MapCodec<BlockColumnFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockColumnFeature.Layer.CODEC.listOf().fieldOf("layers").forGetter(BlockColumnFeature::layers),
            Direction.CODEC.fieldOf("direction").forGetter(BlockColumnFeature::direction),
            BlockPredicate.CODEC.fieldOf("allowed_placement").forGetter(BlockColumnFeature::allowedPlacement),
            Codec.BOOL.fieldOf("prioritize_tip").forGetter(BlockColumnFeature::prioritizeTip)
         )
         .apply(i, BlockColumnFeature::new)
   );

   public static BlockColumnFeature.Layer layer(final IntProvider height, final BlockStateProvider state) {
      return new BlockColumnFeature.Layer(height, state);
   }

   public static BlockColumnFeature simple(final IntProvider height, final BlockStateProvider state) {
      return new BlockColumnFeature(List.of(layer(height, state)), Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, false);
   }

   @Override
   public MapCodec<BlockColumnFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      int layerCount = this.layers.size();
      int[] layerHeights = new int[layerCount];
      int totalHeight = 0;

      for (int i = 0; i < layerCount; i++) {
         layerHeights[i] = this.layers.get(i).height().sample(random);
         totalHeight += layerHeights[i];
      }

      if (totalHeight == 0) {
         return false;
      }

      BlockPos.MutableBlockPos placePos = origin.mutable();
      BlockPos.MutableBlockPos nextPos = placePos.mutable().move(this.direction);

      for (int y = 0; y < totalHeight; y++) {
         if (!this.allowedPlacement.test(level, nextPos)) {
            truncate(layerHeights, totalHeight, y, this.prioritizeTip);
            break;
         }

         nextPos.move(this.direction);
      }

      for (int i = 0; i < layerCount; i++) {
         int count = layerHeights[i];
         if (count != 0) {
            BlockColumnFeature.Layer layer = this.layers.get(i);

            for (int y = 0; y < count; y++) {
               level.setBlock(placePos, layer.state().getState(level, random, placePos), 2);
               placePos.move(this.direction);
            }
         }
      }

      return true;
   }

   private static void truncate(final int[] layerHeights, final int totalHeight, final int newHeight, final boolean prioritizeTip) {
      int amountToRemove = totalHeight - newHeight;
      int direction = prioritizeTip ? 1 : -1;
      int start = prioritizeTip ? 0 : layerHeights.length - 1;
      int end = prioritizeTip ? layerHeights.length : -1;

      for (int i = start; i != end && amountToRemove > 0; i += direction) {
         int thisLayer = layerHeights[i];
         int toRemoveFromLayer = Math.min(thisLayer, amountToRemove);
         amountToRemove -= toRemoveFromLayer;
         layerHeights[i] -= toRemoveFromLayer;
      }
   }

   public record Layer(IntProvider height, BlockStateProvider state) {
      public static final Codec<BlockColumnFeature.Layer> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               IntProviders.NON_NEGATIVE_CODEC.fieldOf("height").forGetter(BlockColumnFeature.Layer::height),
               BlockStateProvider.CODEC.fieldOf("provider").forGetter(BlockColumnFeature.Layer::state)
            )
            .apply(i, BlockColumnFeature.Layer::new)
      );
   }
}
