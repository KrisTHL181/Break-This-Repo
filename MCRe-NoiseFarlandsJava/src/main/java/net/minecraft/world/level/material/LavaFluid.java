package net.minecraft.world.level.material;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public abstract class LavaFluid extends FlowingFluid {
    public static final int LIGHT_EMISSION = 15;
    public static final float MIN_LEVEL_CUTOFF = 0.44444445F;

    @Override
    public Fluid getFlowing() {
        return Fluids.FLOWING_LAVA;
    }

    @Override
    public Fluid getSource() {
        return Fluids.LAVA;
    }

    @Override
    public Item getBucket() {
        return Items.LAVA_BUCKET;
    }

    @Override
    public void animateTick(final Level level, final BlockPos pos, final FluidState fluidState, final RandomSource random) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir() && !level.getBlockState(above).isSolidRender()) {
            if (random.nextInt(100) == 0) {
                double xx = pos.getX() + random.nextDouble();
                double yy = pos.getY() + 1.0;
                double zz = pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.LAVA, xx, yy, zz, 0.0, 0.0, 0.0);
                level.playLocalSound(
                    xx, yy, zz, SoundEvents.LAVA_POP, SoundSource.AMBIENT, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false
                );
            }

            if (random.nextInt(200) == 0) {
                level.playLocalSound(
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    SoundEvents.LAVA_AMBIENT,
                    SoundSource.AMBIENT,
                    0.2F + random.nextFloat() * 0.2F,
                    0.9F + random.nextFloat() * 0.15F,
                    false
                );
            }
        }
    }

    @Override
    public void randomTick(final ServerLevel level, final BlockPos pos, final FluidState fluidState, final RandomSource random) {
        if (level.canSpreadFireAround(pos)) {
            int passes = random.nextInt(3);
            if (passes > 0) {
                BlockPos testPos = pos;

                for (int pass = 0; pass < passes; pass++) {
                    testPos = testPos.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                    if (!level.isLoaded(testPos)) {
                        return;
                    }

                    BlockState blockState = level.getBlockState(testPos);
                    if (blockState.isAir()) {
                        if (this.hasFlammableNeighbours(level, testPos)) {
                            level.setBlockAndUpdate(testPos, BaseFireBlock.getState(level, testPos));
                            return;
                        }
                    } else if (blockState.blocksMotion()) {
                        return;
                    }
                }
            } else {
                for (int i = 0; i < 3; i++) {
                    BlockPos testPos = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                    if (!level.isLoaded(testPos)) {
                        return;
                    }

                    if (level.isEmptyBlock(testPos.above()) && this.isFlammable(level, testPos)) {
                        level.setBlockAndUpdate(testPos.above(), BaseFireBlock.getState(level, testPos));
                    }
                }
            }
        }
    }

    @Override
    protected void entityInside(final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier) {
        effectApplier.apply(InsideBlockEffectType.CLEAR_FREEZE);
        effectApplier.apply(InsideBlockEffectType.LAVA_IGNITE);
        effectApplier.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
    }

    private boolean hasFlammableNeighbours(final LevelReader level, final BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (this.isFlammable(level, pos.relative(direction))) {
                return true;
            }
        }

        return false;
    }

    private boolean isFlammable(final LevelReader level, final BlockPos pos) {
        return level.isInsideBuildHeight(pos.getY()) && !level.hasChunkAt(pos) ? false : level.getBlockState(pos).ignitedByLava();
    }

    @Override
    public @Nullable ParticleOptions getDripParticle() {
        return ParticleTypes.DRIPPING_LAVA;
    }

    @Override
    protected void beforeDestroyingBlock(final LevelAccessor level, final BlockPos pos, final BlockState state) {
        this.fizz(level, pos);
    }

    @Override
    public int getSlopeFindDistance(final LevelReader level) {
        return isFastLava(level) ? 4 : 2;
    }

    @Override
    public BlockState createLegacyBlock(final FluidState fluidState) {
        return Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
    }

    @Override
    public boolean isSame(final Fluid other) {
        return other == Fluids.LAVA || other == Fluids.FLOWING_LAVA;
    }

    @Override
    public int getDropOff(final LevelReader level) {
        return isFastLava(level) ? 1 : 2;
    }

    @Override
    public boolean canBeReplacedWith(final FluidState state, final BlockGetter level, final BlockPos pos, final Fluid other, final Direction direction) {
        return state.getHeight(level, pos) >= 0.44444445F && other.is(FluidTags.WATER);
    }

    @Override
    public int getTickDelay(final LevelReader level) {
        return isFastLava(level) ? 10 : 30;
    }

    @Override
    public int getSpreadDelay(final Level level, final BlockPos pos, final FluidState oldFluidState, final FluidState newFluidState) {
        int result = this.getTickDelay(level);
        if (!oldFluidState.isEmpty()
            && !newFluidState.isEmpty()
            && !oldFluidState.getValue(FALLING)
            && !newFluidState.getValue(FALLING)
            && newFluidState.getHeight(level, pos) > oldFluidState.getHeight(level, pos)
            && level.getRandom().nextInt(4) != 0) {
            result *= 4;
        }

        return result;
    }

    private void fizz(final LevelAccessor level, final BlockPos pos) {
        level.levelEvent(1501, pos, 0);
    }

    @Override
    protected boolean canConvertToSource(final ServerLevel level) {
        return level.getGameRules().get(GameRules.LAVA_SOURCE_CONVERSION);
    }

    @Override
    protected void spreadTo(final LevelAccessor level, final BlockPos pos, final BlockState state, final Direction direction, final FluidState target) {
        if (direction == Direction.DOWN) {
            FluidState fluidState = level.getFluidState(pos);
            if (this.is(FluidTags.LAVA) && fluidState.is(FluidTags.WATER)) {
                if (state.getBlock() instanceof LiquidBlock) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                }

                this.fizz(level, pos);
                return;
            }
        }

        super.spreadTo(level, pos, state, direction, target);
    }

    @Override
    protected boolean isRandomlyTicking() {
        return true;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
    }

    private static boolean isFastLava(final LevelReader level) {
        return level.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA);
    }

    public static class Flowing extends LavaFluid {
        @Override
        protected void createFluidStateDefinition(final StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(final FluidState fluidState) {
            return fluidState.getValue(LEVEL);
        }

        @Override
        public boolean isSource(final FluidState fluidState) {
            return false;
        }
    }

    public static class Source extends LavaFluid {
        @Override
        public int getAmount(final FluidState fluidState) {
            return 8;
        }

        @Override
        public boolean isSource(final FluidState fluidState) {
            return true;
        }
    }
}