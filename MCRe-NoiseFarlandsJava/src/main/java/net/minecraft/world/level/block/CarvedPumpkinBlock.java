package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class CarvedPumpkinBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CarvedPumpkinBlock> CODEC = simpleCodec(CarvedPumpkinBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private @Nullable BlockPattern snowGolemBase;
    private @Nullable BlockPattern snowGolemFull;
    private @Nullable BlockPattern ironGolemBase;
    private @Nullable BlockPattern ironGolemFull;
    private @Nullable BlockPattern copperGolemBase;
    private @Nullable BlockPattern copperGolemFull;
    private static final Predicate<BlockState> PUMPKINS_PREDICATE = input -> input.is(Blocks.CARVED_PUMPKIN) || input.is(Blocks.JACK_O_LANTERN);

    @Override
    public MapCodec<? extends CarvedPumpkinBlock> codec() {
        return CODEC;
    }

    protected CarvedPumpkinBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            this.trySpawnGolem(level, pos);
        }
    }

    public boolean canSpawnGolem(final LevelReader level, final BlockPos topPos) {
        return this.getOrCreateSnowGolemBase().find(level, topPos) != null
            || this.getOrCreateIronGolemBase().find(level, topPos) != null
            || this.getOrCreateCopperGolemBase().find(level, topPos) != null;
    }

    private void trySpawnGolem(final Level level, final BlockPos topPos) {
        BlockPattern.BlockPatternMatch snowGolemMatch = this.getOrCreateSnowGolemFull().find(level, topPos);
        if (snowGolemMatch != null) {
            SnowGolem snowGolem = EntityTypes.SNOW_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
            if (snowGolem != null) {
                spawnGolemInWorld(level, snowGolemMatch, snowGolem, snowGolemMatch.getBlock(0, 2, 0).getPos());
                return;
            }
        }

        BlockPattern.BlockPatternMatch ironGolemMatch = this.getOrCreateIronGolemFull().find(level, topPos);
        if (ironGolemMatch != null) {
            IronGolem ironGolem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
            if (ironGolem != null) {
                ironGolem.setPlayerCreated(true);
                spawnGolemInWorld(level, ironGolemMatch, ironGolem, ironGolemMatch.getBlock(1, 2, 0).getPos());
                return;
            }
        }

        BlockPattern.BlockPatternMatch copperGolemMatch = this.getOrCreateCopperGolemFull().find(level, topPos);
        if (copperGolemMatch != null) {
            CopperGolem copperGolem = EntityTypes.COPPER_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
            if (copperGolem != null) {
                spawnGolemInWorld(level, copperGolemMatch, copperGolem, copperGolemMatch.getBlock(0, 0, 0).getPos());
                this.replaceCopperBlockWithChest(level, copperGolemMatch);
                copperGolem.spawn(this.getWeatherStateFromPattern(copperGolemMatch));
            }
        }
    }

    private WeatheringCopper.WeatherState getWeatherStateFromPattern(final BlockPattern.BlockPatternMatch copperGolemMatch) {
        BlockState state = copperGolemMatch.getBlock(0, 1, 0).getState();
        return state.getBlock() instanceof WeatheringCopper copper
            ? copper.getAge()
            : Optional.ofNullable(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(state.getBlock()))
                .filter(weatheringCopper -> weatheringCopper instanceof WeatheringCopper)
                .map(weatheringCopper -> (WeatheringCopper)weatheringCopper)
                .orElse((WeatheringCopper)Blocks.COPPER_BLOCK.weathering().unaffected())
                .getAge();
    }

    private static void spawnGolemInWorld(final Level level, final BlockPattern.BlockPatternMatch match, final Entity golem, final BlockPos spawnPos) {
        clearPatternBlocks(level, match);
        golem.snapTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.05, spawnPos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(golem);

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
        }

        updatePatternBlocks(level, match);
    }

    public static void clearPatternBlocks(final Level level, final BlockPattern.BlockPatternMatch match) {
        for (int x = 0; x < match.getWidth(); x++) {
            for (int y = 0; y < match.getHeight(); y++) {
                BlockInWorld block = match.getBlock(x, y, 0);
                level.setBlock(block.getPos(), Blocks.AIR.defaultBlockState(), 2);
                level.levelEvent(2001, block.getPos(), Block.getId(block.getState()));
            }
        }
    }

    public static void updatePatternBlocks(final Level level, final BlockPattern.BlockPatternMatch match) {
        for (int x = 0; x < match.getWidth(); x++) {
            for (int y = 0; y < match.getHeight(); y++) {
                BlockInWorld block = match.getBlock(x, y, 0);
                level.updateNeighborsAt(block.getPos(), Blocks.AIR);
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private BlockPattern getOrCreateSnowGolemBase() {
        if (this.snowGolemBase == null) {
            this.snowGolemBase = BlockPatternBuilder.start()
                .aisle(" ", "#", "#")
                .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK)))
                .build();
        }

        return this.snowGolemBase;
    }

    private BlockPattern getOrCreateSnowGolemFull() {
        if (this.snowGolemFull == null) {
            this.snowGolemFull = BlockPatternBuilder.start()
                .aisle("^", "#", "#")
                .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
                .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK)))
                .build();
        }

        return this.snowGolemFull;
    }

    private BlockPattern getOrCreateIronGolemBase() {
        if (this.ironGolemBase == null) {
            this.ironGolemBase = BlockPatternBuilder.start()
                .aisle("~ ~", "###", "~#~")
                .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
                .where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir))
                .build();
        }

        return this.ironGolemBase;
    }

    private BlockPattern getOrCreateIronGolemFull() {
        if (this.ironGolemFull == null) {
            this.ironGolemFull = BlockPatternBuilder.start()
                .aisle("~^~", "###", "~#~")
                .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
                .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
                .where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir))
                .build();
        }

        return this.ironGolemFull;
    }

    private BlockPattern getOrCreateCopperGolemBase() {
        if (this.copperGolemBase == null) {
            this.copperGolemBase = BlockPatternBuilder.start().aisle(" ", "#").where('#', BlockInWorld.hasState(block -> block.is(BlockTags.COPPER))).build();
        }

        return this.copperGolemBase;
    }

    private BlockPattern getOrCreateCopperGolemFull() {
        if (this.copperGolemFull == null) {
            this.copperGolemFull = BlockPatternBuilder.start()
                .aisle("^", "#")
                .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
                .where('#', BlockInWorld.hasState(block -> block.is(BlockTags.COPPER)))
                .build();
        }

        return this.copperGolemFull;
    }

    public void replaceCopperBlockWithChest(final Level level, final BlockPattern.BlockPatternMatch match) {
        BlockInWorld copperBlock = match.getBlock(0, 1, 0);
        BlockInWorld pumpkinBlock = match.getBlock(0, 0, 0);
        Direction facing = pumpkinBlock.getState().getValue(FACING);
        BlockState blockState = CopperChestBlock.getFromCopperBlock(copperBlock.getState().getBlock(), facing, level, copperBlock.getPos());
        level.setBlock(copperBlock.getPos(), blockState, 2);
    }
}