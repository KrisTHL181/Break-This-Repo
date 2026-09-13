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
    protected BaseEntityBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Override
    protected boolean triggerEvent(final BlockState state, final Level level, final BlockPos pos, final int b0, final int b1) {
        super.triggerEvent(state, level, pos, b0, b1);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? false : blockEntity.triggerEvent(b0, b1);
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(final BlockState state, final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
    }

    protected static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
        final BlockEntityType<A> actual, final BlockEntityType<E> expected, final @Nullable BlockEntityTicker<? super E> ticker
    ) {
        return expected == actual ? (BlockEntityTicker<A>)ticker : null;
    }
}
