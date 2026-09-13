package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SnifferEggBlock extends Block {
    public static final MapCodec<SnifferEggBlock> CODEC = simpleCodec(SnifferEggBlock::new);
    public static final int MAX_HATCH_LEVEL = 2;
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    private static final int REGULAR_HATCH_TIME_TICKS = 24000;
    private static final int BOOSTED_HATCH_TIME_TICKS = 12000;
    private static final int RANDOM_HATCH_OFFSET_TICKS = 300;
    private static final VoxelShape SHAPE = Block.column(14.0, 12.0, 0.0, 16.0);

    @Override
    public MapCodec<SnifferEggBlock> codec() {
        return CODEC;
    }

    public SnifferEggBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    public int getHatchLevel(final BlockState state) {
        return state.getValue(HATCH);
    }

    private boolean isReadyToHatch(final BlockState state) {
        return this.getHatchLevel(state) == 2;
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos position, final RandomSource random) {
        if (!this.isReadyToHatch(state)) {
            level.playSound(null, position, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            level.setBlock(position, state.setValue(HATCH, this.getHatchLevel(state) + 1), 2);
        } else {
            level.playSound(null, position, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            level.destroyBlock(position, false);
            Sniffer sniffer = EntityTypes.SNIFFER.create(level, EntitySpawnReason.BREEDING);
            if (sniffer != null) {
                Vec3 spawnAt = Vec3.atCenterOf(position);
                sniffer.setBaby(true);
                sniffer.snapTo(spawnAt.x(), spawnAt.y(), spawnAt.z(), Mth.wrapDegrees(level.getRandom().nextFloat() * 360.0F), 0.0F);
                level.addFreshEntity(sniffer);
            }
        }
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        boolean boosted = hatchBoost(level, pos);
        if (!level.isClientSide() && boosted) {
            level.levelEvent(3009, pos, 0);
        }

        int hatchTime = boosted ? 12000 : 24000;
        int progressionTickDelay = hatchTime / 3;
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(state));
        level.scheduleTick(pos, this, progressionTickDelay + level.getRandom().nextInt(300));
    }

    @Override
    public boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }

    public static boolean hatchBoost(final BlockGetter level, final BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.SNIFFER_EGG_HATCH_BOOST);
    }
}