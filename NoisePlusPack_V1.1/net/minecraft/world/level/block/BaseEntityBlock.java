package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class BaseEntityBlock extends Block implements EntityBlock {
   protected BaseEntityBlock(BlockBehaviour.Properties p_49224_) {
      super(p_49224_);
   }

   @Override
   protected abstract MapCodec<? extends BaseEntityBlock> codec();

   @Override
   protected boolean triggerEvent(BlockState p_49226_, Level p_49227_, BlockPos p_49228_, int p_49229_, int p_49230_) {
      super.triggerEvent(p_49226_, p_49227_, p_49228_, p_49229_, p_49230_);
      BlockEntity blockentity = p_49227_.getBlockEntity(p_49228_);
      return blockentity == null ? false : blockentity.triggerEvent(p_49229_, p_49230_);
   }

   @Override
   protected @Nullable MenuProvider getMenuProvider(BlockState p_49234_, Level p_49235_, BlockPos p_49236_) {
      BlockEntity blockentity = p_49235_.getBlockEntity(p_49236_);
      return blockentity instanceof MenuProvider ? (MenuProvider)blockentity : null;
   }

   protected static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
      BlockEntityType<A> p_152133_, BlockEntityType<E> p_152134_, BlockEntityTicker<? super E> p_152135_
   ) {
      return p_152134_ == p_152133_ ? p_152135_ : null;
   }
}
