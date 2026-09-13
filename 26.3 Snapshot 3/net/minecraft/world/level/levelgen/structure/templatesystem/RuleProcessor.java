package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

public class RuleProcessor implements StructureProcessor {
   public static final MapCodec<RuleProcessor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ProcessorRule.CODEC.listOf().fieldOf("rules").forGetter(p -> p.rules)).apply(i, RuleProcessor::new)
   );
   private final ImmutableList<ProcessorRule> rules;

   public RuleProcessor(final List<? extends ProcessorRule> rules) {
      this.rules = ImmutableList.copyOf(rules);
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
      RandomSource random = RandomSource.create(Mth.getSeed(processedBlockInfo.pos()));
      UnmodifiableIterator var8 = this.rules.iterator();

      while (var8.hasNext()) {
         ProcessorRule rule = (ProcessorRule)var8.next();
         if (rule.test(level, processedBlockInfo.state(), templateRelativePos, processedBlockInfo.pos(), referencePos, random)) {
            return new StructureTemplate.StructureBlockInfo(
               processedBlockInfo.pos(), rule.getOutputState(), rule.getOutputTag(random, processedBlockInfo.nbt())
            );
         }
      }

      return processedBlockInfo;
   }

   @Override
   public MapCodec<RuleProcessor> codec() {
      return MAP_CODEC;
   }
}
