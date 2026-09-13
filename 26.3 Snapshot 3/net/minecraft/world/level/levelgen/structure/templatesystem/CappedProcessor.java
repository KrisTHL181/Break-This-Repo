package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.ServerLevelAccessor;

public class CappedProcessor implements StructureProcessor {
   public static final MapCodec<CappedProcessor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            StructureProcessorType.SINGLE_CODEC.fieldOf("delegate").forGetter(c -> c.delegate),
            IntProviders.POSITIVE_CODEC.fieldOf("limit").forGetter(c -> c.limit)
         )
         .apply(i, CappedProcessor::new)
   );
   private final StructureProcessor delegate;
   private final IntProvider limit;

   public CappedProcessor(final StructureProcessor delegate, final IntProvider limit) {
      this.delegate = delegate;
      this.limit = limit;
   }

   @Override
   public MapCodec<CappedProcessor> codec() {
      return MAP_CODEC;
   }

   @Override
   public boolean evaluatesEntirePieceState() {
      return true;
   }

   @Override
   public final List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
      final ServerLevelAccessor level,
      final BlockPos position,
      final BlockPos referencePos,
      final List<StructureTemplate.StructureBlockInfo> originalBlockInfoList,
      final List<StructureTemplate.StructureBlockInfo> processedBlockInfoList,
      final StructurePlaceSettings settings
   ) {
      if (this.limit.maxInclusive() != 0 && !processedBlockInfoList.isEmpty()) {
         if (originalBlockInfoList.size() != processedBlockInfoList.size()) {
            Util.logAndPauseIfInIde(
               "Original block info list not in sync with processed list, skipping processing. Original size: "
                  + originalBlockInfoList.size()
                  + ", Processed size: "
                  + processedBlockInfoList.size()
            );
            return processedBlockInfoList;
         }

         RandomSource random = RandomSource.createThreadLocalInstance(level.getLevel().getSeed()).forkPositional().at(position);
         int maxToReplace = Math.min(this.limit.sample(random), processedBlockInfoList.size());
         if (maxToReplace < 1) {
            return processedBlockInfoList;
         }

         IntArrayList indices = Util.toShuffledList(IntStream.range(0, processedBlockInfoList.size()), random);
         IntIterator indexIterator = indices.intIterator();
         int replaced = 0;

         while (indexIterator.hasNext() && replaced < maxToReplace) {
            int index = indexIterator.nextInt();
            StructureTemplate.StructureBlockInfo originalBlockInfo = originalBlockInfoList.get(index);
            StructureTemplate.StructureBlockInfo processedBlockInfo = processedBlockInfoList.get(index);
            StructureTemplate.StructureBlockInfo maybeAltered = this.delegate
               .processBlock(level, position, referencePos, originalBlockInfo.pos(), processedBlockInfo, settings);
            if (maybeAltered != null && !processedBlockInfo.equals(maybeAltered)) {
               replaced++;
               processedBlockInfoList.set(index, maybeAltered);
            }
         }

         return processedBlockInfoList;
      } else {
         return processedBlockInfoList;
      }
   }
}
