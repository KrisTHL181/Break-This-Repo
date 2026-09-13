package net.minecraft.world.level.block;

import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CaveVines {
   VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);
   BooleanProperty BERRIES = BlockStateProperties.BERRIES;

   static InteractionResult use(final Entity sourceEntity, final BlockState state, final Level level, final BlockPos pos) {
      if (state.getValue(BERRIES)) {
         if (level instanceof ServerLevel serverLevel) {
            Block.dropFromBlockInteractLootTable(
               serverLevel,
               BuiltInLootTables.HARVEST_CAVE_VINE,
               state,
               level.getBlockEntity(pos),
               null,
               sourceEntity,
               (serverlvl, itemStack) -> Block.popResource(serverlvl, pos, itemStack)
            );
            float pitch = Mth.randomBetween(serverLevel.getRandom(), 0.8F, 1.2F);
            serverLevel.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, pitch);
            BlockState newState = state.setValue(BERRIES, false);
            serverLevel.setBlock(pos, newState, 2);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   static boolean hasGlowBerries(final BlockState state) {
      return state.hasProperty(BERRIES) && state.getValue(BERRIES);
   }

   static ToIntFunction<BlockState> emission(final int lightEmission) {
      return state -> state.getValue(BlockStateProperties.BERRIES) ? lightEmission : 0;
   }
}
