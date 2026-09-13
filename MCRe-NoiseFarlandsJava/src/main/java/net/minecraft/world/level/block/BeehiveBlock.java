package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BeehiveBlock extends BaseEntityBlock {
    public static final MapCodec<BeehiveBlock> CODEC = simpleCodec(BeehiveBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty HONEY_LEVEL = BlockStateProperties.LEVEL_HONEY;
    public static final int MAX_HONEY_LEVELS = 5;

    @Override
    public MapCodec<BeehiveBlock> codec() {
        return CODEC;
    }

    public BeehiveBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HONEY_LEVEL, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
        return state.getValue(HONEY_LEVEL);
    }

    @Override
    public void playerDestroy(
        final Level level,
        final Player player,
        final BlockPos pos,
        final BlockState state,
        final @Nullable BlockEntity blockEntity,
        final ItemStack destroyedWith
    ) {
        super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
        if (!level.isClientSide() && blockEntity instanceof BeehiveBlockEntity beehiveBlockEntity) {
            if (!EnchantmentHelper.hasTag(destroyedWith, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING)) {
                beehiveBlockEntity.emptyAllLivingFromHive(player, state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                Containers.updateNeighboursAfterDestroy(state, level, pos);
                this.angerNearbyBees(level, pos);
            }

            CriteriaTriggers.BEE_NEST_DESTROYED.trigger((ServerPlayer)player, state, destroyedWith, beehiveBlockEntity.getOccupantCount());
        }
    }

    @Override
    protected void onExplosionHit(
        final BlockState state, final ServerLevel level, final BlockPos pos, final Explosion explosion, final BiConsumer<ItemStack, BlockPos> onHit
    ) {
        super.onExplosionHit(state, level, pos, explosion, onHit);
        this.angerNearbyBees(level, pos);
    }

    private void angerNearbyBees(final Level level, final BlockPos pos) {
        AABB areaAroundBeehive = new AABB(pos).inflate(8.0, 6.0, 8.0);
        List<Bee> beesToAnger = level.getEntitiesOfClass(Bee.class, areaAroundBeehive);
        if (!beesToAnger.isEmpty()) {
            List<Player> playersToBeAngryAt = level.getEntitiesOfClass(Player.class, areaAroundBeehive);
            if (playersToBeAngryAt.isEmpty()) {
                return;
            }

            for (Bee bee : beesToAnger) {
                if (bee.getTarget() == null) {
                    Player angerTarget = Util.getRandom(playersToBeAngryAt, level.getRandom());
                    bee.setTarget(angerTarget);
                }
            }
        }
    }

    public static void dropHoneycomb(
        final ServerLevel level,
        final ItemStack tool,
        final BlockState blockState,
        final @Nullable BlockEntity blockEntity,
        final @Nullable Entity entity,
        final BlockPos pos
    ) {
        dropFromBlockInteractLootTable(
            level, BuiltInLootTables.HARVEST_BEEHIVE, blockState, blockEntity, tool, entity, (serverLevel, stack) -> popResource(serverLevel, pos, stack)
        );
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
        int honeyLevel = state.getValue(HONEY_LEVEL);
        boolean hiveEmptied = false;
        if (honeyLevel >= 5) {
            Item item = itemStack.getItem();
            if (level instanceof ServerLevel serverLevel && itemStack.is(Items.SHEARS)) {
                dropHoneycomb(serverLevel, itemStack, state, level.getBlockEntity(pos), player, pos);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                hiveEmptied = true;
                level.gameEvent(player, GameEvent.SHEAR, pos);
            } else if (itemStack.is(Items.GLASS_BOTTLE)) {
                itemStack.shrink(1);
                level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (itemStack.isEmpty()) {
                    player.setItemInHand(hand, new ItemStack(Items.HONEY_BOTTLE));
                } else if (!player.getInventory().add(new ItemStack(Items.HONEY_BOTTLE))) {
                    player.drop(new ItemStack(Items.HONEY_BOTTLE), false);
                }

                hiveEmptied = true;
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }

            if (!level.isClientSide() && hiveEmptied) {
                player.awardStat(Stats.ITEM_USED.get(item));
            }
        }

        if (hiveEmptied) {
            if (!CampfireBlock.isSmokeyPos(level, pos)) {
                if (this.hiveContainsBees(level, pos)) {
                    this.angerNearbyBees(level, pos);
                }

                this.releaseBeesAndResetHoneyLevel(level, state, pos, player, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
            } else {
                this.resetHoneyLevel(level, state, pos);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
        }
    }

    private boolean hiveContainsBees(final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveBlockEntity ? !beehiveBlockEntity.isEmpty() : false;
    }

    public void releaseBeesAndResetHoneyLevel(
        final Level level,
        final BlockState state,
        final BlockPos pos,
        final @Nullable Player player,
        final BeehiveBlockEntity.BeeReleaseStatus beeReleaseStatus
    ) {
        this.resetHoneyLevel(level, state, pos);
        if (level.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveBlockEntity) {
            beehiveBlockEntity.emptyAllLivingFromHive(player, state, beeReleaseStatus);
        }
    }

    public void resetHoneyLevel(final Level level, final BlockState state, final BlockPos pos) {
        level.setBlock(pos, state.setValue(HONEY_LEVEL, 0), 3);
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (state.getValue(HONEY_LEVEL) >= 5) {
            for (int i = 0; i < random.nextInt(1) + 1; i++) {
                this.trySpawnDripParticles(level, pos, state);
            }
        }
    }

    private void trySpawnDripParticles(final Level level, final BlockPos pos, final BlockState state) {
        if (state.getFluidState().isEmpty() && !(level.getRandom().nextFloat() < 0.3F)) {
            VoxelShape collisionShape = state.getCollisionShape(level, pos);
            double topSideHeight = collisionShape.max(Direction.Axis.Y);
            if (topSideHeight >= 1.0 && !state.is(BlockTags.IMPERMEABLE)) {
                double bottomSideHeight = collisionShape.min(Direction.Axis.Y);
                if (bottomSideHeight > 0.0) {
                    this.spawnParticle(level, pos, collisionShape, pos.getY() + bottomSideHeight - 0.05);
                } else {
                    BlockPos below = pos.below();
                    BlockState belowState = level.getBlockState(below);
                    VoxelShape belowShape = belowState.getCollisionShape(level, below);
                    double belowTopSideHeight = belowShape.max(Direction.Axis.Y);
                    if ((belowTopSideHeight < 1.0 || !belowState.isCollisionShapeFullBlock(level, below)) && belowState.getFluidState().isEmpty()) {
                        this.spawnParticle(level, pos, collisionShape, pos.getY() - 0.05);
                    }
                }
            }
        }
    }

    private void spawnParticle(final Level level, final BlockPos pos, final VoxelShape dripShape, final double height) {
        this.spawnFluidParticle(
            level,
            pos.getX() + dripShape.min(Direction.Axis.X),
            pos.getX() + dripShape.max(Direction.Axis.X),
            pos.getZ() + dripShape.min(Direction.Axis.Z),
            pos.getZ() + dripShape.max(Direction.Axis.Z),
            height
        );
    }

    private void spawnFluidParticle(final Level level, final double x1, final double x2, final double z1, final double z2, final double y) {
        level.addParticle(
            ParticleTypes.DRIPPING_HONEY, Mth.lerp(level.getRandom().nextDouble(), x1, x2), y, Mth.lerp(level.getRandom().nextDouble(), z1, z2), 0.0, 0.0, 0.0
        );
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HONEY_LEVEL, FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        return new BeehiveBlockEntity(worldPosition, blockState);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, BlockEntityTypes.BEEHIVE, BeehiveBlockEntity::serverTick);
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
        if (level instanceof ServerLevel serverLevel
            && player.preventsBlockDrops()
            && serverLevel.getGameRules().get(GameRules.BLOCK_DROPS)
            && level.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveBlockEntity) {
            int honeyLevel = state.getValue(HONEY_LEVEL);
            boolean hasBees = !beehiveBlockEntity.isEmpty();
            if (hasBees || honeyLevel > 0) {
                ItemStack itemStack = new ItemStack(this);
                itemStack.applyComponents(beehiveBlockEntity.collectComponents());
                itemStack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(HONEY_LEVEL, honeyLevel));
                ItemEntity entity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(final BlockState state, final LootParams.Builder params) {
        Entity entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (entity instanceof PrimedTnt
            || entity instanceof Creeper
            || entity instanceof WitherSkull
            || entity instanceof WitherBoss
            || entity instanceof MinecartTNT) {
            BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (blockEntity instanceof BeehiveBlockEntity beehiveBlockEntity) {
                beehiveBlockEntity.emptyAllLivingFromHive(null, state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
            }
        }

        return super.getDrops(state, params);
    }

    @Override
    protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state, final boolean includeData) {
        ItemStack itemStack = super.getCloneItemStack(level, pos, state, includeData);
        if (includeData) {
            itemStack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(HONEY_LEVEL, state.getValue(HONEY_LEVEL)));
        }

        return itemStack;
    }

    @Override
    protected BlockState updateShape(
        final BlockState state,
        final LevelReader level,
        final ScheduledTickAccess ticks,
        final BlockPos pos,
        final Direction directionToNeighbour,
        final BlockPos neighbourPos,
        final BlockState neighbourState,
        final RandomSource random
    ) {
        if (level.getBlockState(neighbourPos).getBlock() instanceof FireBlock && level.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveBlockEntity) {
            beehiveBlockEntity.emptyAllLivingFromHive(null, state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}