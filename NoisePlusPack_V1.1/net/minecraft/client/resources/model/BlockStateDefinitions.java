package net.minecraft.client.resources.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockStateDefinitions {
   private static final StateDefinition<Block, BlockState> ITEM_FRAME_FAKE_DEFINITION = createItemFrameFakeState();
   private static final StateDefinition<Block, BlockState> GLOW_ITEM_FRAME_FAKE_DEFINITION = createItemFrameFakeState();
   private static final Identifier GLOW_ITEM_FRAME_LOCATION = Identifier.withDefaultNamespace("glow_item_frame");
   private static final Identifier ITEM_FRAME_LOCATION = Identifier.withDefaultNamespace("item_frame");
   private static final Map<Identifier, StateDefinition<Block, BlockState>> STATIC_DEFINITIONS = Map.of(
      ITEM_FRAME_LOCATION, ITEM_FRAME_FAKE_DEFINITION, GLOW_ITEM_FRAME_LOCATION, GLOW_ITEM_FRAME_FAKE_DEFINITION
   );

   private static StateDefinition<Block, BlockState> createItemFrameFakeState() {
      return new StateDefinition.Builder<Block, BlockState>(Blocks.AIR).add(BlockStateProperties.MAP).create(Block::defaultBlockState, BlockState::new);
   }

   public static BlockState getItemFrameFakeState(boolean p_397641_, boolean p_392045_) {
      return (p_397641_ ? GLOW_ITEM_FRAME_FAKE_DEFINITION : ITEM_FRAME_FAKE_DEFINITION).any().setValue(BlockStateProperties.MAP, p_392045_);
   }

   static Function<Identifier, StateDefinition<Block, BlockState>> definitionLocationToBlockStateMapper() {
      Map<Identifier, StateDefinition<Block, BlockState>> map = new HashMap<>(STATIC_DEFINITIONS);

      for (Block block : BuiltInRegistries.BLOCK) {
         map.put(block.builtInRegistryHolder().key().identifier(), block.getStateDefinition());
      }

      return map::get;
   }
}
