package net.minecraft.world.level.levelgen.carver;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class CarvingContext extends WorldGenerationContext {
    private final RegistryAccess registryAccess;
    private final NoiseChunk noiseChunk;
    private final RandomState randomState;
    private final SurfaceRules.RuleSource surfaceRule;

    public CarvingContext(
        final NoiseBasedChunkGenerator generator,
        final RegistryAccess registryAccess,
        final LevelHeightAccessor heightAccessor,
        final NoiseChunk noiseChunk,
        final RandomState randomState,
        final SurfaceRules.RuleSource surfaceRule
    ) {
        super(generator, heightAccessor);
        this.registryAccess = registryAccess;
        this.noiseChunk = noiseChunk;
        this.randomState = randomState;
        this.surfaceRule = surfaceRule;
    }

    @Deprecated
    public Optional<BlockState> topMaterial(
        final Function<BlockPos, Holder<Biome>> biomeGetter, final ChunkAccess chunk, final BlockPos pos, final boolean underFluid
    ) {
        return this.randomState.surfaceSystem().topMaterial(this.surfaceRule, this, biomeGetter, chunk, this.noiseChunk, pos, underFluid);
    }

    @Deprecated
    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public RandomState randomState() {
        return this.randomState;
    }
}