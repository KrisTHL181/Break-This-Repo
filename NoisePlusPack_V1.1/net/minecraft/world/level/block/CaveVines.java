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

   static InteractionResult use(Entity p_270738_, BlockState p_270772_, Level p_270721_, BlockPos p_270587_) {
      if (p_270772_.getValue(BERRIES)) {
         if (p_270721_ instanceof ServerLevel serverlevel) {
            Block.dropFromBlockInteractLootTable(
               serverlevel,
               BuiltInLootTables.HARVEST_CAVE_VINE,
               p_270772_,
               p_270721_.getBlockEntity(p_270587_),
               null,
               p_270738_,
               (p_422089_, p_422090_) -> Block.popResource(p_422089_, p_270587_, p_422090_)
            );
            float f = Mth.randomBetween(serverlevel.random, 0.8F, 1.2F);
            serverlevel.playSound(null, p_270587_, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, f);
            BlockState blockstate = p_270772_.setValue(BERRIES, false);
            serverlevel.setBlock(p_270587_, blockstate, 2);
            serverlevel.gameEvent(GameEvent.BLOCK_CHANGE, p_270587_, GameEvent.Context.of(p_270738_, blockstate));
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   static boolean hasGlowBerries(BlockState p_152952_) {
      return p_152952_.hasProperty(BERRIES) && p_152952_.getValue(BERRIES);
   }

   static ToIntFunction<BlockState> emission(int p_181218_) {
      return p_181216_ -> p_181216_.getValue(BlockStateProperties.BERRIES) ? p_181218_ : 0;
   }
}
