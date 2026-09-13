package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;

public class JigsawBlock extends Block implements EntityBlock, GameMasterBlock {
    public static final MapCodec<JigsawBlock> CODEC = simpleCodec(JigsawBlock::new);
    public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;

    @Override
    public MapCodec<JigsawBlock> codec() {
        return CODEC;
    }

    protected JigsawBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ORIENTATION, FrontAndTop.NORTH_UP));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION);
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(ORIENTATION, rotation.rotation().rotate(state.getValue(ORIENTATION)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.setValue(ORIENTATION, mirror.rotation().rotate(state.getValue(ORIENTATION)));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        Direction front = context.getClickedFace();
        Direction top;
        if (front.getAxis() == Direction.Axis.Y) {
            top = context.getHorizontalDirection().getOpposite();
        } else {
            top = Direction.UP;
        }

        return this.defaultBlockState().setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(front, top));
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        return new JigsawBlockEntity(worldPosition, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof JigsawBlockEntity jigsawBlockEntity && player.canUseGameMasterBlocks()) {
            player.openJigsawBlock(jigsawBlockEntity);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    public static boolean canAttach(final StructureTemplate.JigsawBlockInfo source, final StructureTemplate.JigsawBlockInfo target) {
        Direction sourceFront = getFrontFacing(source.info().state());
        Direction targetFront = getFrontFacing(target.info().state());
        Direction sourceTop = getTopFacing(source.info().state());
        Direction targetTop = getTopFacing(target.info().state());
        JigsawBlockEntity.JointType jointType = source.jointType();
        boolean rollable = jointType == JigsawBlockEntity.JointType.ROLLABLE;
        return sourceFront == targetFront.getOpposite() && (rollable || sourceTop == targetTop) && source.target().equals(target.name());
    }

    public static Direction getFrontFacing(final BlockState state) {
        return state.getValue(ORIENTATION).front();
    }

    public static Direction getTopFacing(final BlockState state) {
        return state.getValue(ORIENTATION).top();
    }
}