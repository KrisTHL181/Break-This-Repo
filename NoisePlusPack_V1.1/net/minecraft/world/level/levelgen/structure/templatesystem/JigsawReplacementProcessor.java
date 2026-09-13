package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class JigsawReplacementProcessor extends StructureProcessor {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<JigsawReplacementProcessor> CODEC = MapCodec.unit(() -> JigsawReplacementProcessor.INSTANCE);
   public static final JigsawReplacementProcessor INSTANCE = new JigsawReplacementProcessor();

   private JigsawReplacementProcessor() {
   }

   @Override
   public StructureTemplate.@Nullable StructureBlockInfo processBlock(
      LevelReader p_74127_,
      BlockPos p_74128_,
      BlockPos p_74129_,
      StructureTemplate.StructureBlockInfo p_74130_,
      StructureTemplate.StructureBlockInfo p_74131_,
      StructurePlaceSettings p_74132_
   ) {
      BlockState blockstate = p_74131_.state();
      if (!blockstate.is(Blocks.JIGSAW) || SharedConstants.DEBUG_KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN) {
         return p_74131_;
      }

      if (p_74131_.nbt() == null) {
         LOGGER.warn("Jigsaw block at {} is missing nbt, will not replace", p_74128_);
         return p_74131_;
      }

      String s = p_74131_.nbt().getStringOr("final_state", "minecraft:air");

      BlockState blockstate1;
      try {
         BlockStateParser.BlockResult blockstateparser$blockresult = BlockStateParser.parseForBlock(p_74127_.holderLookup(Registries.BLOCK), s, true);
         blockstate1 = blockstateparser$blockresult.blockState();
      } catch (CommandSyntaxException commandsyntaxexception) {
         LOGGER.error("Failed to parse jigsaw replacement state '{}' at {}: {}", new Object[]{s, p_74128_, commandsyntaxexception.getMessage()});
         return null;
      }

      return blockstate1.is(Blocks.STRUCTURE_VOID) ? null : new StructureTemplate.StructureBlockInfo(p_74131_.pos(), blockstate1, null);
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.JIGSAW_REPLACEMENT;
   }
}
