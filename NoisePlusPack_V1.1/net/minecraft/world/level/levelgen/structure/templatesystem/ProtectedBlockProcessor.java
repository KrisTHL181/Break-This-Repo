package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.jspecify.annotations.Nullable;

public class ProtectedBlockProcessor extends StructureProcessor {
   public final TagKey<Block> cannotReplace;
   public static final MapCodec<ProtectedBlockProcessor> CODEC = TagKey.hashedCodec(Registries.BLOCK)
      .xmap(ProtectedBlockProcessor::new, p_205053_ -> p_205053_.cannotReplace)
      .fieldOf("value");

   public ProtectedBlockProcessor(TagKey<Block> p_205051_) {
      this.cannotReplace = p_205051_;
   }

   @Override
   public StructureTemplate.@Nullable StructureBlockInfo processBlock(
      LevelReader p_163755_,
      BlockPos p_163756_,
      BlockPos p_163757_,
      StructureTemplate.StructureBlockInfo p_163758_,
      StructureTemplate.StructureBlockInfo p_163759_,
      StructurePlaceSettings p_163760_
   ) {
      return Feature.isReplaceable(this.cannotReplace).test(p_163755_.getBlockState(p_163759_.pos())) ? p_163759_ : null;
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.PROTECTED_BLOCKS;
   }
}
