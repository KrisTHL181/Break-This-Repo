package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TargetBlock extends Block {
    public static final MapCodec<TargetBlock> CODEC = simpleCodec(TargetBlock::new);
    private static final IntegerProperty OUTPUT_POWER = BlockStateProperties.POWER;
    private static final int ACTIVATION_TICKS_ARROWS = 20;
    private static final int ACTIVATION_TICKS_OTHER = 8;

    @Override
    public MapCodec<TargetBlock> codec() {
        return CODEC;
    }

    public TargetBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OUTPUT_POWER, 0));
    }

    @Override
    protected void onProjectileHit(final Level level, final BlockState state, final BlockHitResult hitResult, final Projectile projectile) {
        int outputStrength = updateRedstoneOutput(level, state, hitResult, projectile);
        if (projectile.getOwner() instanceof ServerPlayer playerOwner) {
            playerOwner.awardStat(Stats.TARGET_HIT);
            CriteriaTriggers.TARGET_BLOCK_HIT.trigger(playerOwner, projectile, hitResult.getLocation(), outputStrength);
        }
    }

    private static int updateRedstoneOutput(final LevelAccessor level, final BlockState state, final BlockHitResult hitResult, final Entity entity) {
        int redstoneStrength = getRedstoneStrength(hitResult, hitResult.getLocation());
        int duration = entity instanceof AbstractArrow ? 20 : 8;
        if (!level.getBlockTicks().hasScheduledTick(hitResult.getBlockPos(), state.getBlock())) {
            setOutputPower(level, state, redstoneStrength, hitResult.getBlockPos(), duration);
        }

        return redstoneStrength;
    }

    private static int getRedstoneStrength(final BlockHitResult hitResult, final Vec3 hitLocation) {
        Direction hitDirection = hitResult.getDirection();
        double distX = Math.abs(Mth.frac(hitLocation.x) - 0.5);
        double distY = Math.abs(Mth.frac(hitLocation.y) - 0.5);
        double distZ = Math.abs(Mth.frac(hitLocation.z) - 0.5);
        Direction.Axis axis = hitDirection.getAxis();
        double distance;
        if (axis == Direction.Axis.Y) {
            distance = Math.max(distX, distZ);
        } else if (axis == Direction.Axis.Z) {
            distance = Math.max(distX, distY);
        } else {
            distance = Math.max(distY, distZ);
        }

        return Math.max(1, Mth.ceil(15.0 * Mth.clamp((0.5 - distance) / 0.5, 0.0, 1.0)));
    }

    private static void setOutputPower(final LevelAccessor level, final BlockState state, final int outputStrength, final BlockPos pos, final int duration) {
        level.setBlock(pos, state.setValue(OUTPUT_POWER, outputStrength), 3);
        level.scheduleTick(pos, state.getBlock(), duration);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (state.getValue(OUTPUT_POWER) != 0) {
            level.setBlock(pos, state.setValue(OUTPUT_POWER, 0), 3);
        }
    }

    @Override
    protected int ownSignal(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(OUTPUT_POWER);
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OUTPUT_POWER);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            if (state.getValue(OUTPUT_POWER) > 0 && !level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.setBlock(pos, state.setValue(OUTPUT_POWER, 0), 18);
            }
        }
    }
}