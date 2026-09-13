package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlackstoneReplaceProcessor implements StructureProcessor {
   public static final MapCodec<BlackstoneReplaceProcessor> MAP_CODEC = MapCodec.unit(() -> BlackstoneReplaceProcessor.INSTANCE);
   public static final BlackstoneReplaceProcessor INSTANCE = new BlackstoneReplaceProcessor();
   private final Map<Block, Block> replacements = Util.make(Maps.newHashMap(), map -> {
      map.put(Blocks.COBBLESTONE, Blocks.BLACKSTONE);
      map.put(Blocks.MOSSY_COBBLESTONE, Blocks.BLACKSTONE);
      map.put(Blocks.STONE, Blocks.POLISHED_BLACKSTONE);
      map.put(Blocks.STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
      map.put(Blocks.MOSSY_STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
      map.put(Blocks.COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
      map.put(Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
      map.put(Blocks.STONE_STAIRS, Blocks.POLISHED_BLACKSTONE_STAIRS);
      map.put(Blocks.STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
      map.put(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
      map.put(Blocks.COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
      map.put(Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
      map.put(Blocks.SMOOTH_STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
      map.put(Blocks.STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
      map.put(Blocks.STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
      map.put(Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
      map.put(Blocks.STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
      map.put(Blocks.MOSSY_STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
      map.put(Blocks.COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
      map.put(Blocks.MOSSY_COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
      map.put(Blocks.CHISELED_STONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE);
      map.put(Blocks.CRACKED_STONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
      map.put(Blocks.IRON_BARS, Blocks.IRON_CHAIN);
   });

   private BlackstoneReplaceProcessor() {
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
      Block newBlock = this.replacements.get(processedBlockInfo.state().getBlock());
      if (newBlock == null) {
         return processedBlockInfo;
      }

      BlockState oldState = processedBlockInfo.state();
      BlockState newState = newBlock.defaultBlockState();
      if (oldState.hasProperty(StairBlock.FACING)) {
         newState = newState.setValue(StairBlock.FACING, oldState.getValue(StairBlock.FACING));
      }

      if (oldState.hasProperty(StairBlock.HALF)) {
         newState = newState.setValue(StairBlock.HALF, oldState.getValue(StairBlock.HALF));
      }

      if (oldState.hasProperty(SlabBlock.TYPE)) {
         newState = newState.setValue(SlabBlock.TYPE, oldState.getValue(SlabBlock.TYPE));
      }

      return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), newState, processedBlockInfo.nbt());
   }

   @Override
   public MapCodec<BlackstoneReplaceProcessor> codec() {
      return MAP_CODEC;
   }
}
