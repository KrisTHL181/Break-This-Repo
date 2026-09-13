package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class UntintedParticleLeavesBlock extends LeavesBlock {
    public static final MapCodec<UntintedParticleLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("leaf_particle_chance").forGetter(e -> e.leafParticleChance),
                ParticleTypes.CODEC.fieldOf("leaf_particle").forGetter(e -> e.leafParticle),
                propertiesCodec()
            )
            .apply(i, UntintedParticleLeavesBlock::new)
    );
    protected final ParticleOptions leafParticle;

    public UntintedParticleLeavesBlock(final float leafParticleChance, final ParticleOptions leafParticle, final BlockBehaviour.Properties properties) {
        super(leafParticleChance, properties);
        this.leafParticle = leafParticle;
    }

    @Override
    protected void spawnFallingLeavesParticle(final Level level, final BlockPos pos, final RandomSource random) {
        ParticleUtils.spawnParticleBelow(level, pos, random, this.leafParticle);
    }

    @Override
    public MapCodec<UntintedParticleLeavesBlock> codec() {
        return CODEC;
    }
}