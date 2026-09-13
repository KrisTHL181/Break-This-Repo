package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ComposterBlock extends Block implements WorldlyContainerHolder {
    public static final MapCodec<ComposterBlock> CODEC = simpleCodec(ComposterBlock::new);
    public static final int READY = 8;
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 7;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_COMPOSTER;
    public static final Object2FloatMap<ItemLike> COMPOSTABLES = new Object2FloatOpenHashMap<>();
    private static final int HOLE_WIDTH = 12;
    private static final VoxelShape[] SHAPES = Util.make(
        () -> {
            VoxelShape[] shapes = Block.boxes(
                8, level -> Shapes.join(Shapes.block(), Block.column(12.0, Math.clamp(1 + level * 2, 2, 16), 16.0), BooleanOp.ONLY_FIRST)
            );
            shapes[8] = shapes[7];
            return shapes;
        }
    );

    @Override
    public MapCodec<ComposterBlock> codec() {
        return CODEC;
    }

    public static void bootStrap() {
        COMPOSTABLES.defaultReturnValue(-1.0F);
        float low = 0.3F;
        float lowMid = 0.5F;
        float mid = 0.65F;
        float midHigh = 0.85F;
        float high = 1.0F;
        add(0.3F, Items.JUNGLE_LEAVES);
        add(0.3F, Items.OAK_LEAVES);
        add(0.3F, Items.SPRUCE_LEAVES);
        add(0.3F, Items.DARK_OAK_LEAVES);
        add(0.3F, Items.PALE_OAK_LEAVES);
        add(0.3F, Items.ACACIA_LEAVES);
        add(0.3F, Items.CHERRY_LEAVES);
        add(0.3F, Items.BIRCH_LEAVES);
        add(0.3F, Items.AZALEA_LEAVES);
        add(0.3F, Items.MANGROVE_LEAVES);
        add(0.3F, Items.OAK_SAPLING);
        add(0.3F, Items.SPRUCE_SAPLING);
        add(0.3F, Items.BIRCH_SAPLING);
        add(0.3F, Items.JUNGLE_SAPLING);
        add(0.3F, Items.ACACIA_SAPLING);
        add(0.3F, Items.CHERRY_SAPLING);
        add(0.3F, Items.DARK_OAK_SAPLING);
        add(0.3F, Items.PALE_OAK_SAPLING);
        add(0.3F, Items.MANGROVE_PROPAGULE);
        add(0.3F, Items.BEETROOT_SEEDS);
        add(0.3F, Items.DRIED_KELP);
        add(0.3F, Items.SHORT_GRASS);
        add(0.3F, Items.KELP);
        add(0.3F, Items.MELON_SEEDS);
        add(0.3F, Items.PUMPKIN_SEEDS);
        add(0.3F, Items.SEAGRASS);
        add(0.3F, Items.SWEET_BERRIES);
        add(0.3F, Items.GLOW_BERRIES);
        add(0.3F, Items.WHEAT_SEEDS);
        add(0.3F, Items.MOSS_CARPET);
        add(0.3F, Items.PALE_MOSS_CARPET);
        add(0.3F, Items.PALE_HANGING_MOSS);
        add(0.3F, Items.PINK_PETALS);
        add(0.3F, Items.WILDFLOWERS);
        add(0.3F, Items.LEAF_LITTER);
        add(0.3F, Items.SMALL_DRIPLEAF);
        add(0.3F, Items.HANGING_ROOTS);
        add(0.3F, Items.MANGROVE_ROOTS);
        add(0.3F, Items.TORCHFLOWER_SEEDS);
        add(0.3F, Items.PITCHER_POD);
        add(0.3F, Items.FIREFLY_BUSH);
        add(0.3F, Items.BUSH);
        add(0.3F, Items.CACTUS_FLOWER);
        add(0.3F, Items.DRY_SHORT_GRASS);
        add(0.3F, Items.DRY_TALL_GRASS);
        add(0.5F, Items.DRIED_KELP_BLOCK);
        add(0.5F, Items.TALL_GRASS);
        add(0.5F, Items.FLOWERING_AZALEA_LEAVES);
        add(0.5F, Items.CACTUS);
        add(0.5F, Items.SUGAR_CANE);
        add(0.5F, Items.VINE);
        add(0.5F, Items.NETHER_SPROUTS);
        add(0.5F, Items.WEEPING_VINES);
        add(0.5F, Items.TWISTING_VINES);
        add(0.5F, Items.MELON_SLICE);
        add(0.5F, Items.GLOW_LICHEN);
        add(0.65F, Items.SEA_PICKLE);
        add(0.65F, Items.LILY_PAD);
        add(0.65F, Items.PUMPKIN);
        add(0.65F, Items.CARVED_PUMPKIN);
        add(0.65F, Items.MELON);
        add(0.65F, Items.APPLE);
        add(0.65F, Items.BEETROOT);
        add(0.65F, Items.CARROT);
        add(0.65F, Items.COCOA_BEANS);
        add(0.65F, Items.POTATO);
        add(0.65F, Items.WHEAT);
        add(0.65F, Items.BROWN_MUSHROOM);
        add(0.65F, Items.RED_MUSHROOM);
        add(0.65F, Items.MUSHROOM_STEM);
        add(0.65F, Items.CRIMSON_FUNGUS);
        add(0.65F, Items.WARPED_FUNGUS);
        add(0.65F, Items.NETHER_WART);
        add(0.65F, Items.CRIMSON_ROOTS);
        add(0.65F, Items.WARPED_ROOTS);
        add(0.65F, Items.SHROOMLIGHT);
        add(0.65F, Items.DANDELION);
        add(0.65F, Items.POPPY);
        add(0.65F, Items.BLUE_ORCHID);
        add(0.65F, Items.ALLIUM);
        add(0.65F, Items.AZURE_BLUET);
        add(0.65F, Items.RED_TULIP);
        add(0.65F, Items.ORANGE_TULIP);
        add(0.65F, Items.WHITE_TULIP);
        add(0.65F, Items.PINK_TULIP);
        add(0.65F, Items.OXEYE_DAISY);
        add(0.65F, Items.CORNFLOWER);
        add(0.65F, Items.LILY_OF_THE_VALLEY);
        add(0.65F, Items.WITHER_ROSE);
        add(0.65F, Items.OPEN_EYEBLOSSOM);
        add(0.65F, Items.CLOSED_EYEBLOSSOM);
        add(0.65F, Items.FERN);
        add(0.65F, Items.SUNFLOWER);
        add(0.65F, Items.LILAC);
        add(0.65F, Items.ROSE_BUSH);
        add(0.65F, Items.PEONY);
        add(0.65F, Items.LARGE_FERN);
        add(0.65F, Items.SPORE_BLOSSOM);
        add(0.65F, Items.AZALEA);
        add(0.65F, Items.MOSS_BLOCK);
        add(0.65F, Items.PALE_MOSS_BLOCK);
        add(0.65F, Items.BIG_DRIPLEAF);
        add(0.85F, Items.HAY_BLOCK);
        add(0.85F, Items.BROWN_MUSHROOM_BLOCK);
        add(0.85F, Items.RED_MUSHROOM_BLOCK);
        add(0.85F, Items.NETHER_WART_BLOCK);
        add(0.85F, Items.WARPED_WART_BLOCK);
        add(0.85F, Items.FLOWERING_AZALEA);
        add(0.85F, Items.BREAD);
        add(0.85F, Items.BAKED_POTATO);
        add(0.85F, Items.COOKIE);
        add(0.85F, Items.TORCHFLOWER);
        add(0.85F, Items.PITCHER_PLANT);
        add(1.0F, Items.CAKE);
        add(1.0F, Items.PUMPKIN_PIE);
    }

    private static void add(final float value, final ItemLike item) {
        COMPOSTABLES.put(item.asItem(), value);
    }

    public ComposterBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    public static void handleFill(final Level level, final BlockPos pos, final boolean success) {
        BlockState state = level.getBlockState(pos);
        level.playLocalSound(pos, success ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        double centerHeight = state.getShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5) + 0.03125;
        double sideOffsetPixels = 2.0;
        double sideOffset = 0.1875;
        double width = 0.625;
        RandomSource random = level.getRandom();

        for (int i = 0; i < 10; i++) {
            double xa = random.nextGaussian() * 0.02;
            double ya = random.nextGaussian() * 0.02;
            double za = random.nextGaussian() * 0.02;
            level.addParticle(
                ParticleTypes.COMPOSTER,
                pos.getX() + 0.1875 + 0.625 * random.nextFloat(),
                pos.getY() + centerHeight + random.nextFloat() * (1.0 - centerHeight),
                pos.getZ() + 0.1875 + 0.625 * random.nextFloat(),
                xa,
                ya,
                za
            );
        }
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES[state.getValue(LEVEL)];
    }

    @Override
    protected VoxelShape getInteractionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES[0];
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (state.getValue(LEVEL) == 7) {
            level.scheduleTick(pos, state.getBlock(), 20);
        }
    }

    @Override
    protected InteractionResult useItemOn(
        final ItemStack itemStack,
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final InteractionHand hand,
        final BlockHitResult hitResult
    ) {
        int fillLevel = state.getValue(LEVEL);
        if (fillLevel < 8 && COMPOSTABLES.containsKey(itemStack.getItem())) {
            if (fillLevel < 7 && !level.isClientSide()) {
                BlockState newState = addItem(player, state, level, pos, itemStack);
                level.levelEvent(1500, pos, state != newState ? 1 : 0);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                itemStack.consume(1, player);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
    ) {
        int fillLevel = state.getValue(LEVEL);
        if (fillLevel == 8) {
            extractProduce(player, state, level, pos);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    public static BlockState insertItem(
        final Entity sourceEntity, final BlockState state, final ServerLevel level, final ItemStack itemStack, final BlockPos pos
    ) {
        int fillLevel = state.getValue(LEVEL);
        if (fillLevel < 7 && COMPOSTABLES.containsKey(itemStack.getItem())) {
            BlockState newState = addItem(sourceEntity, state, level, pos, itemStack);
            itemStack.shrink(1);
            return newState;
        } else {
            return state;
        }
    }

    public static BlockState extractProduce(final Entity sourceEntity, final BlockState state, final Level level, final BlockPos pos) {
        if (!level.isClientSide()) {
            Vec3 itemPos = Vec3.atLowerCornerWithOffset(pos, 0.5, 1.01, 0.5).offsetRandomXZ(level.getRandom(), 0.7F);
            ItemEntity entity = new ItemEntity(level, itemPos.x(), itemPos.y(), itemPos.z(), new ItemStack(Items.BONE_MEAL));
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }

        BlockState emptyState = empty(sourceEntity, state, level, pos);
        level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        return emptyState;
    }

    private static BlockState empty(final @Nullable Entity sourceEntity, final BlockState state, final LevelAccessor level, final BlockPos pos) {
        BlockState newState = state.setValue(LEVEL, 0);
        level.setBlock(pos, newState, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
        return newState;
    }

    private static BlockState addItem(
        final @Nullable Entity sourceEntity, final BlockState state, final LevelAccessor level, final BlockPos pos, final ItemStack itemStack
    ) {
        int fillLevel = state.getValue(LEVEL);
        float chance = COMPOSTABLES.getFloat(itemStack.getItem());
        if ((fillLevel != 0 || !(chance > 0.0F)) && !(level.getRandom().nextDouble() < chance)) {
            return state;
        }

        int newLevel = fillLevel + 1;
        BlockState newState = state.setValue(LEVEL, newLevel);
        level.setBlock(pos, newState, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
        if (newLevel == 7) {
            level.scheduleTick(pos, state.getBlock(), 20);
        }

        return newState;
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (state.getValue(LEVEL) == 7) {
            level.setBlock(pos, state.cycle(LEVEL), 3);
            level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
        return state.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }

    @Override
    public WorldlyContainer getContainer(final BlockState state, final LevelAccessor level, final BlockPos pos) {
        int contentLevel = state.getValue(LEVEL);
        if (contentLevel == 8) {
            return new ComposterBlock.OutputContainer(state, level, pos, new ItemStack(Items.BONE_MEAL));
        } else {
            return contentLevel < 7 ? new ComposterBlock.InputContainer(state, level, pos) : new ComposterBlock.EmptyContainer();
        }
    }

    private static class EmptyContainer extends SimpleContainer implements WorldlyContainer {
        public EmptyContainer() {
            super(0);
        }

        @Override
        public int[] getSlotsForFace(final Direction direction) {
            return new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(final int slot, final ItemStack itemStack, final @Nullable Direction direction) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(final int slot, final ItemStack itemStack, final Direction direction) {
            return false;
        }
    }

    private static class InputContainer extends SimpleContainer implements WorldlyContainer {
        private final BlockState state;
        private final LevelAccessor level;
        private final BlockPos pos;
        private boolean changed;

        public InputContainer(final BlockState state, final LevelAccessor level, final BlockPos pos) {
            super(1);
            this.state = state;
            this.level = level;
            this.pos = pos;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(final Direction direction) {
            return direction == Direction.UP ? new int[]{0} : new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(final int slot, final ItemStack itemStack, final @Nullable Direction direction) {
            return !this.changed && direction == Direction.UP && ComposterBlock.COMPOSTABLES.containsKey(itemStack.getItem());
        }

        @Override
        public boolean canTakeItemThroughFace(final int slot, final ItemStack itemStack, final Direction direction) {
            return false;
        }

        @Override
        public void setChanged() {
            ItemStack contents = this.getItem(0);
            if (!contents.isEmpty()) {
                this.changed = true;
                BlockState newState = ComposterBlock.addItem(null, this.state, this.level, this.pos, contents);
                this.level.levelEvent(1500, this.pos, newState != this.state ? 1 : 0);
                this.removeItemNoUpdate(0);
            }
        }
    }

    private static class OutputContainer extends SimpleContainer implements WorldlyContainer {
        private final BlockState state;
        private final LevelAccessor level;
        private final BlockPos pos;
        private boolean changed;

        public OutputContainer(final BlockState state, final LevelAccessor level, final BlockPos pos, final ItemStack contents) {
            super(contents);
            this.state = state;
            this.level = level;
            this.pos = pos;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(final Direction direction) {
            return direction == Direction.DOWN ? new int[]{0} : new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(final int slot, final ItemStack itemStack, final @Nullable Direction direction) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(final int slot, final ItemStack itemStack, final Direction direction) {
            return !this.changed && direction == Direction.DOWN && itemStack.is(Items.BONE_MEAL);
        }

        @Override
        public void setChanged() {
            ComposterBlock.empty(null, this.state, this.level, this.pos);
            this.changed = true;
        }
    }
}