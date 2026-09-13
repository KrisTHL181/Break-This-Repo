package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MangroveLeavesBlock extends TintedParticleLeavesBlock implements BonemealableBlock {
    public static final MapCodec<MangroveLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("leaf_particle_chance").forGetter(e -> e.leafParticleChance), propertiesCodec())
            .apply(i, MangroveLeavesBlock::new)
    );

    @Override
    public MapCodec<MangroveLeavesBlock> codec() {
        return CODEC;
    }

    public MangroveLeavesBlock(final float leafParticleChance, final BlockBehaviour.Properties properties) {
        super(leafParticleChance, properties);
    }

    @Override
    public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
        return level.getBlockState(pos.below()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
        level.setBlock(pos.below(), MangrovePropaguleBlock.createNewHangingPropagule(), 2);
    }

    @Override
    public BlockPos getParticlePos(final BlockPos blockPos) {
        return blockPos.below();
    }
}