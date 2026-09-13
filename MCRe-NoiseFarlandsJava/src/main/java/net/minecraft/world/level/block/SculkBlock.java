package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class SculkBlock extends DropExperienceBlock implements SculkBehaviour {
    public static final MapCodec<SculkBlock> CODEC = simpleCodec(SculkBlock::new);

    @Override
    public MapCodec<SculkBlock> codec() {
        return CODEC;
    }

    public SculkBlock(final BlockBehaviour.Properties properties) {
        super(ConstantInt.of(1), properties);
    }

    @Override
    public int attemptUseCharge(
        final SculkSpreader.ChargeCursor cursor,
        final LevelAccessor level,
        final BlockPos originPos,
        final RandomSource random,
        final SculkSpreader spreader,
        final boolean spreadVein
    ) {
        int charge = cursor.getCharge();
        if (charge != 0 && random.nextInt(spreader.chargeDecayRate()) == 0) {
            BlockPos chargePos = cursor.getPos();
            boolean isCloseToCatalyst = chargePos.closerThan(originPos, spreader.noGrowthRadius());
            if (!isCloseToCatalyst && canPlaceGrowth(level, chargePos)) {
                int xpPerGrowthSpawn = spreader.growthSpawnCost();
                if (random.nextInt(xpPerGrowthSpawn) < charge) {
                    BlockPos growthPlacement = chargePos.above();
                    BlockState growthState = this.getRandomGrowthState(level, growthPlacement, random, spreader.isWorldGeneration());
                    level.setBlock(growthPlacement, growthState, 3);
                    level.playSound(null, chargePos, growthState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                return Math.max(0, charge - xpPerGrowthSpawn);
            } else {
                return random.nextInt(spreader.additionalDecayRate()) != 0
                    ? charge
                    : charge - (isCloseToCatalyst ? 1 : getDecayPenalty(spreader, chargePos, originPos, charge));
            }
        } else {
            return charge;
        }
    }

    private static int getDecayPenalty(final SculkSpreader spreader, final BlockPos pos, final BlockPos originPos, final int charge) {
        int noGrowthRadius = spreader.noGrowthRadius();
        float outerDistanceSquared = Mth.square((float)Math.sqrt(pos.distSqr(originPos)) - noGrowthRadius);
        int maxReachSquared = Mth.square(24 - noGrowthRadius);
        float distanceFactor = Math.min(1.0F, outerDistanceSquared / maxReachSquared);
        return Math.max(1, (int)(charge * distanceFactor * 0.5F));
    }

    private BlockState getRandomGrowthState(final LevelAccessor level, final BlockPos pos, final RandomSource random, final boolean isWorldGen) {
        BlockState state;
        if (random.nextInt(11) == 0) {
            state = Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, isWorldGen);
        } else {
            state = Blocks.SCULK_SENSOR.defaultBlockState();
        }

        return state.hasProperty(BlockStateProperties.WATERLOGGED) && !level.getFluidState(pos).isEmpty()
            ? state.setValue(BlockStateProperties.WATERLOGGED, true)
            : state;
    }

    private static boolean canPlaceGrowth(final LevelAccessor level, final BlockPos pos) {
        BlockState stateAbove = level.getBlockState(pos.above());
        if (stateAbove.isAir() || stateAbove.is(Blocks.WATER) && stateAbove.getFluidState().is(Fluids.WATER)) {
            int growthCount = 0;

            for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 2, 4))) {
                BlockState state = level.getBlockState(blockPos);
                if (state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.SCULK_SHRIEKER)) {
                    growthCount++;
                }

                if (growthCount > 2) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canChangeBlockStateOnSpread() {
        return false;
    }
}