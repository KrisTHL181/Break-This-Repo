package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import org.jspecify.annotations.Nullable;

public interface EntityBlock {
   @Nullable BlockEntity newBlockEntity(BlockPos var1, BlockState var2);

   default <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_153212_, BlockState p_153213_, BlockEntityType<T> p_153214_) {
      return null;
   }

   default <T extends BlockEntity> @Nullable GameEventListener getListener(ServerLevel p_221121_, T p_221122_) {
      return p_221122_ instanceof GameEventListener.Provider<?> provider ? provider.getListener() : null;
   }
}
