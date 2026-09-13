package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GameTestHelper {
    private final GameTestInfo testInfo;
    private boolean finalCheckAdded;

    public GameTestHelper(final GameTestInfo testInfo) {
        this.testInfo = testInfo;
    }

    public GameTestAssertException assertionException(final Component description) {
        return new GameTestAssertException(description, this.testInfo.getTick());
    }

    public GameTestAssertException assertionException(final String descriptionId, final Object... arguments) {
        return this.assertionException(Component.translatableEscape(descriptionId, arguments));
    }

    public GameTestAssertPosException assertionException(final BlockPos pos, final Component description) {
        return new GameTestAssertPosException(description, this.absolutePos(pos), pos, this.testInfo.getTick());
    }

    public GameTestAssertPosException assertionException(final BlockPos pos, final String descriptionId, final Object... arguments) {
        return this.assertionException(pos, Component.translatableEscape(descriptionId, arguments));
    }

    public ServerLevel getLevel() {
        return this.testInfo.getLevel();
    }

    public BlockState getBlockState(final BlockPos pos) {
        return this.getLevel().getBlockState(this.absolutePos(pos));
    }

    public <T extends BlockEntity> T getBlockEntity(final BlockPos pos, final Class<T> type) {
        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.absolutePos(pos));
        if (blockEntity == null) {
            throw this.assertionException(pos, "test.error.missing_block_entity");
        } else if (type.isInstance(blockEntity)) {
            return type.cast(blockEntity);
        } else {
            throw this.assertionException(pos, "test.error.wrong_block_entity", blockEntity.typeHolder().getRegisteredName());
        }
    }

    public void killAllEntities() {
        this.killAllEntitiesOfClass(Entity.class);
    }

    public void killAllEntitiesOfClass(final Class<? extends Entity> baseClass) {
        AABB bounds = this.getBounds();
        List<? extends Entity> entities = this.getLevel().getEntitiesOfClass(baseClass, bounds.inflate(1.0), mob -> !(mob instanceof Player));
        entities.forEach(entity -> entity.kill(this.getLevel()));
    }

    public ItemEntity spawnItem(final Item item, final Vec3 pos) {
        ServerLevel level = this.getLevel();
        Vec3 absoluteVec = this.absoluteVec(pos);
        ItemEntity itemEntity = new ItemEntity(level, absoluteVec.x, absoluteVec.y, absoluteVec.z, new ItemStack(item, 1));
        itemEntity.setDeltaMovement(0.0, 0.0, 0.0);
        level.addFreshEntity(itemEntity);
        return itemEntity;
    }

    public ItemEntity spawnItem(final Item item, final float x, final float y, final float z) {
        return this.spawnItem(item, new Vec3(x, y, z));
    }

    public ItemEntity spawnItem(final Item item, final BlockPos pos) {
        return this.spawnItem(item, pos.getX(), pos.getY(), pos.getZ());
    }

    public void despawnItem(final BlockPos pos, final double distance) {
        BlockPos absolutePos = this.absolutePos(pos);

        for (ItemEntity entity : this.getLevel().getEntities(EntityTypes.ITEM, new AABB(absolutePos).inflate(distance), Entity::isAlive)) {
            entity.remove(Entity.RemovalReason.KILLED);
        }
    }

    public <E extends Entity> E spawn(final EntityType<E> entityType, final BlockPos pos) {
        return this.spawn(entityType, Vec3.atBottomCenterOf(pos));
    }

    public <E extends Entity> List<E> spawn(final EntityType<E> entityType, final BlockPos pos, final int amount) {
        return this.spawn(entityType, Vec3.atBottomCenterOf(pos), amount);
    }

    public <E extends Entity> List<E> spawn(final EntityType<E> entityType, final Vec3 pos, final int amount) {
        return this.spawnEntity(entityType, pos).spawn(amount);
    }

    public <E extends Entity> E spawn(final EntityType<E> entityType, final Vec3 pos) {
        return this.spawn(entityType, pos, null);
    }

    public <E extends Mob> E spawn(final EntityType<E> entityType, final double x, final double y, final double z, final EntitySpawnReason entitySpawnReason) {
        return this.spawn(entityType, new Vec3(x, y, z), entitySpawnReason);
    }

    public <E extends Entity> E spawn(final EntityType<E> entityType, final BlockPos pos, final @Nullable EntitySpawnReason spawnReason) {
        return this.spawnEntity(entityType, pos).spawnReason(spawnReason).spawn();
    }

    public <E extends Entity> E spawn(final EntityType<E> entityType, final Vec3 pos, final @Nullable EntitySpawnReason spawnReason) {
        return this.spawnEntity(entityType, pos).spawnReason(spawnReason).spawn();
    }

    public <E extends Entity> GameTestEntityBuilder<E> spawnEntity(final EntityType<E> entityType, final Vec3 position) {
        return new GameTestEntityBuilder<>(this, entityType, position);
    }

    public <E extends Entity> GameTestEntityBuilder<E> spawnEntity(final EntityType<E> entityType, final BlockPos position) {
        return this.spawnEntity(entityType, Vec3.atBottomCenterOf(position));
    }

    public <E extends Entity> GameTestEntityBuilder<E> spawnEntity(final EntityType<E> entityType, final int x, final int y, final int z) {
        return this.spawnEntity(entityType, new BlockPos(x, y, z));
    }

    public <E extends Entity> GameTestEntityBuilder<E> spawnEntity(final EntityType<E> entityType, final float x, final float y, final float z) {
        return this.spawnEntity(entityType, new Vec3(x, y, z));
    }

    public <E extends Mob> GameTestMobBuilder<E> spawnMob(final EntityType<E> entityType, final Vec3 position) {
        return new GameTestMobBuilder<>(this, entityType, position);
    }

    public <E extends Mob> GameTestMobBuilder<E> spawnMob(final EntityType<E> entityType, final BlockPos position) {
        return this.spawnMob(entityType, Vec3.atBottomCenterOf(position));
    }

    public <E extends Mob> GameTestMobBuilder<E> spawnMob(final EntityType<E> entityType, final int x, final int y, final int z) {
        return this.spawnMob(entityType, new BlockPos(x, y, z));
    }

    public <E extends Mob> GameTestMobBuilder<E> spawnMob(final EntityType<E> entityType, final float x, final float y, final float z) {
        return this.spawnMob(entityType, new Vec3(x, y, z));
    }

    public void hurt(final Entity entity, final DamageSource source, final float damage) {
        entity.hurtServer(this.getLevel(), source, damage);
    }

    public void kill(final Entity entity) {
        entity.kill(this.getLevel());
    }

    public void discard(final Entity entity) {
        entity.discard();
    }

    public <E extends Entity> E findOneEntity(final EntityType<E> entityType) {
        return this.findClosestEntity(entityType, 0, 0, 0, 2.147483647E9);
    }

    public <E extends Entity> E findClosestEntity(final EntityType<E> entityType, final int x, final int y, final int z, final double distance) {
        List<E> entities = this.findEntities(entityType, x, y, z, distance);
        if (entities.isEmpty()) {
            throw this.assertionException("test.error.expected_entity_around", entityType.getDescription(), x, y, z);
        }

        if (entities.size() > 1) {
            throw this.assertionException("test.error.too_many_entities", entityType.toShortString(), x, y, z, entities.size());
        }

        Vec3 center = this.absoluteVec(new Vec3(x, y, z));
        entities.sort((e1, e2) -> {
            double d1 = e1.position().distanceTo(center);
            double d2 = e2.position().distanceTo(center);
            return Double.compare(d1, d2);
        });
        return entities.get(0);
    }

    public <E extends Entity> List<E> findEntities(final EntityType<E> entityType, final int x, final int y, final int z, final double distance) {
        return this.findEntities(entityType, Vec3.atBottomCenterOf(new BlockPos(x, y, z)), distance);
    }

    public <E extends Entity> List<E> findEntities(final EntityType<E> entityType, final Vec3 pos, final double distance) {
        ServerLevel level = this.getLevel();
        Vec3 absoluteVec = this.absoluteVec(pos);
        AABB structureBounds = this.testInfo.getStructureBounds();
        AABB containedBounds = new AABB(absoluteVec.add(-distance, -distance, -distance), absoluteVec.add(distance, distance, distance));
        return level.getEntities(entityType, structureBounds, e -> e.getBoundingBox().intersects(containedBounds) && e.isAlive());
    }

    public <E extends Entity> E spawn(final EntityType<E> entityType, final int x, final int y, final int z) {
        return this.spawn(entityType, new BlockPos(x, y, z));
    }

    public <E extends Entity> E spawn(final EntityType<E> entityType, final float x, final float y, final float z) {
        return this.spawn(entityType, new Vec3(x, y, z));
    }

    public <E extends Mob> E spawnWithNoFreeWill(final EntityType<E> entityType, final BlockPos pos) {
        return this.spawnMob(entityType, pos).withNoFreeWill().spawn();
    }

    public <E extends Mob> E spawnWithNoFreeWill(final EntityType<E> entityType, final int x, final int y, final int z) {
        return this.spawnWithNoFreeWill(entityType, new BlockPos(x, y, z));
    }

    public <E extends Mob> E spawnWithNoFreeWill(final EntityType<E> entityType, final Vec3 pos) {
        E entity = (E)this.spawn(entityType, pos);
        entity.removeFreeWill();
        return entity;
    }

    public <E extends Mob> E spawnWithNoFreeWill(final EntityType<E> entityType, final float x, final float y, final float z) {
        return this.spawnWithNoFreeWill(entityType, new Vec3(x, y, z));
    }

    public void moveTo(final Mob mob, final float x, final float y, final float z) {
        this.moveTo(mob, new Vec3(x, y, z));
    }

    public void moveTo(final Mob mob, final BlockPos pos) {
        this.moveTo(mob, Vec3.atBottomCenterOf(pos));
    }

    public void moveTo(final Mob mob, final Vec3 pos) {
        Vec3 absoluteVec = this.absoluteVec(pos);
        mob.snapTo(absoluteVec.x, absoluteVec.y, absoluteVec.z, mob.getYRot(), mob.getXRot());
    }

    public GameTestSequence walkTo(final Mob mob, final BlockPos targetPos, final float speedModifier) {
        return this.startSequence().thenExecuteAfter(2, () -> {
            Path path = mob.getNavigation().createPath(this.absolutePos(targetPos), 0);
            mob.getNavigation().moveTo(path, speedModifier);
        });
    }

    public void pressButton(final int x, final int y, final int z) {
        this.pressButton(new BlockPos(x, y, z));
    }

    public void pressButton(final BlockPos buttonPos) {
        this.assertBlockTag(BlockTags.BUTTONS, buttonPos);
        BlockPos absolutePos = this.absolutePos(buttonPos);
        BlockState blockState = this.getLevel().getBlockState(absolutePos);
        ButtonBlock buttonBlock = (ButtonBlock)blockState.getBlock();
        buttonBlock.press(blockState, this.getLevel(), absolutePos, null);
    }

    public void useBlock(final BlockPos relativePos) {
        this.useBlock(relativePos, this.makeMockPlayer(GameType.CREATIVE));
    }

    public void useBlock(final BlockPos relativePos, final Player player) {
        BlockPos absolutePos = this.absolutePos(relativePos);
        this.useBlock(relativePos, player, new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.NORTH, absolutePos, true));
    }

    public void useBlock(final BlockPos relativePos, final Player player, final BlockHitResult hitResult) {
        BlockPos absolutePos = this.absolutePos(relativePos);
        BlockState blockState = this.getLevel().getBlockState(absolutePos);
        InteractionHand hand = InteractionHand.MAIN_HAND;
        InteractionResult itemInteractionResult = blockState.useItemOn(player.getItemInHand(hand), this.getLevel(), player, hand, hitResult);
        if (!itemInteractionResult.consumesAction()) {
            if (!(itemInteractionResult instanceof InteractionResult.TryEmptyHandInteraction)
                || !blockState.useWithoutItem(this.getLevel(), player, hitResult).consumesAction()) {
                UseOnContext context = new UseOnContext(player, hand, hitResult);
                player.getItemInHand(hand).useOn(context);
            }
        }
    }

    public LivingEntity makeAboutToDrown(final LivingEntity entity) {
        entity.setAirSupply(0);
        entity.setHealth(0.25F);
        return entity;
    }

    public LivingEntity withLowHealth(final LivingEntity entity) {
        entity.setHealth(0.25F);
        return entity;
    }

    public Player makeMockPlayer(final GameType gameType) {
        return new Player(this.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public GameType gameMode() {
                return gameType;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
    }

    public Player makeMockServerPlayer(final GameType gameType) {
        var player = new ServerPlayer(
            this.getLevel().getServer(), this.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player"), ClientInformation.createDefault()
        ) {
            @Override
            public GameType gameMode() {
                return gameType;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
        gameType.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    @Deprecated(forRemoval = true)
    public ServerPlayer makeMockServerPlayerInLevel() {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = new ServerPlayer(this.getLevel().getServer(), this.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public GameType gameMode() {
                return GameType.CREATIVE;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        this.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    public void pullLever(final int x, final int y, final int z) {
        this.pullLever(new BlockPos(x, y, z));
    }

    public void pullLever(final BlockPos leverPos) {
        this.assertBlockPresent(Blocks.LEVER, leverPos);
        BlockPos absolutePos = this.absolutePos(leverPos);
        BlockState blockState = this.getLevel().getBlockState(absolutePos);
        LeverBlock leverBlock = (LeverBlock)blockState.getBlock();
        leverBlock.pull(blockState, this.getLevel(), absolutePos, null);
    }

    public void placeBlock(final BlockPos relativePos, final Block block, final Direction relativePlaceDirection) {
        BlockPos pos = this.absolutePos(relativePos);
        Direction placeDirection = this.getAbsoluteDirection(relativePlaceDirection);
        Item item = block.asItem();
        if (item instanceof BlockItem blockItem) {
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, true);
            BlockPlaceContext context = new GameTestHelper.TestBlockPlaceContext(
                this.getLevel(), InteractionHand.MAIN_HAND, item.getDefaultInstance(), hitResult, placeDirection
            );
            blockItem.place(context);
        } else {
            this.fail(Component.translatable("test.error.not_a_block_item"));
        }
    }

    public void placeBlock(final int x, final int y, final int z, final Block block, final Direction placeDirection) {
        this.placeBlock(new BlockPos(x, y, z), block, placeDirection);
    }

    public void pulseRedstone(final BlockPos pos, final long duration) {
        this.setBlock(pos, Blocks.REDSTONE_BLOCK);
        this.runAfterDelay(duration, () -> this.setBlock(pos, Blocks.AIR));
    }

    public void destroyBlock(final BlockPos pos) {
        this.getLevel().destroyBlock(this.absolutePos(pos), false, null);
    }

    public void setBlock(final int x, final int y, final int z, final Block block) {
        this.setBlock(new BlockPos(x, y, z), block);
    }

    public void setBlock(final int x, final int y, final int z, final BlockState state) {
        this.setBlock(new BlockPos(x, y, z), state);
    }

    public void setBlock(final BlockPos blockPos, final Block block) {
        this.setBlock(blockPos, block.defaultBlockState());
    }

    public void setBlock(final BlockPos blockPos, final BlockState state) {
        this.getLevel().setBlock(this.absolutePos(blockPos), state, 3);
    }

    public void setBlock(final BlockPos blockPos, final Block block, final Direction direction) {
        this.setBlock(blockPos, block.defaultBlockState(), direction);
    }

    public void setBlock(final BlockPos blockPos, final BlockState blockState, final Direction direction) {
        BlockState state = blockState;
        if (blockState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state = blockState.setValue(HorizontalDirectionalBlock.FACING, direction);
        }

        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            state = blockState.setValue(BlockStateProperties.FACING, direction);
        }

        this.getLevel().setBlock(this.absolutePos(blockPos), state, 3);
    }

    public void assertBlockPresent(final Block blockType, final int x, final int y, final int z) {
        this.assertBlockPresent(blockType, new BlockPos(x, y, z));
    }

    public void assertBlockPresent(final Block blockType, final BlockPos pos) {
        BlockState state = this.getBlockState(pos);
        this.assertBlock(pos, block -> state.is(blockType), block -> Component.translatable("test.error.expected_block", blockType.getName(), block.getName()));
    }

    public void assertBlockPresent(final Block blockType) {
        AABB aabb = this.getRelativeBounds().contract(1.0, 1.0, 1.0);
        boolean foundBlock = BlockPos.MutableBlockPos.betweenClosedStream(aabb).anyMatch(blockPos -> this.getBlockState(blockPos).is(blockType));
        if (!foundBlock) {
            throw this.assertionException(Component.translatable("test.error.expected_block_present", blockType.getName()));
        }
    }

    public void assertBlockNotPresent(final Block blockType, final int x, final int y, final int z) {
        this.assertBlockNotPresent(blockType, new BlockPos(x, y, z));
    }

    public void assertBlockNotPresent(final Block blockType, final BlockPos pos) {
        this.assertBlock(
            pos, block -> !this.getBlockState(pos).is(blockType), block -> Component.translatable("test.error.unexpected_block", blockType.getName())
        );
    }

    public void assertBlockTag(final TagKey<Block> tag, final BlockPos pos) {
        this.assertBlockState(
            pos,
            state -> state.is(tag),
            state -> Component.translatable("test.error.expected_block_tag", Component.translationArg(tag.location()), state.getBlock().getName())
        );
    }

    public void succeedWhenBlockPresent(final Block block, final int x, final int y, final int z) {
        this.succeedWhenBlockPresent(block, new BlockPos(x, y, z));
    }

    public void succeedWhenBlockPresent(final Block block, final BlockPos pos) {
        this.succeedWhen(() -> this.assertBlockPresent(block, pos));
    }

    public void assertBlock(final BlockPos pos, final Predicate<Block> predicate, final Function<Block, Component> errorMessage) {
        this.assertBlockState(pos, blockState -> predicate.test(blockState.getBlock()), state -> errorMessage.apply(state.getBlock()));
    }

    public <T extends Comparable<T>> void assertBlockProperty(final BlockPos pos, final Property<T> property, final T value) {
        BlockState blockState = this.getBlockState(pos);
        boolean hasProperty = blockState.hasProperty(property);
        if (!hasProperty) {
            throw this.assertionException(pos, "test.error.block_property_missing", property.getName(), value);
        }

        if (!blockState.<T>getValue(property).equals(value)) {
            throw this.assertionException(pos, "test.error.block_property_mismatch", property.getName(), value, blockState.getValue(property));
        }
    }

    public <T extends Comparable<T>> void assertBlockProperty(
        final BlockPos pos, final Property<T> property, final Predicate<T> predicate, final Component errorMessage
    ) {
        this.assertBlockState(pos, blockState -> {
            if (!blockState.hasProperty(property)) {
                return false;
            }

            T value = blockState.getValue(property);
            return predicate.test(value);
        }, state -> errorMessage);
    }

    public void assertBlockState(final BlockPos pos, final BlockState expected) {
        BlockState blockState = this.getBlockState(pos);
        if (!blockState.equals(expected)) {
            throw this.assertionException(pos, "test.error.state_not_equal", expected, blockState);
        }
    }

    public void assertBlockState(final BlockPos pos, final Predicate<BlockState> predicate, final Function<BlockState, Component> errorMessage) {
        BlockState blockState = this.getBlockState(pos);
        if (!predicate.test(blockState)) {
            throw this.assertionException(pos, errorMessage.apply(blockState));
        }
    }

    public <T extends BlockEntity> void assertBlockEntityData(
        final BlockPos pos, final Class<T> type, final Predicate<T> predicate, final Supplier<Component> errorMessage
    ) {
        T blockEntity = this.getBlockEntity(pos, type);
        if (!predicate.test(blockEntity)) {
            throw this.assertionException(pos, errorMessage.get());
        }
    }

    public void assertRedstoneSignal(final BlockPos pos, final Direction direction, final IntPredicate levelPredicate, final Supplier<Component> errorMessage) {
        BlockPos blockPos = this.absolutePos(pos);
        ServerLevel level = this.getLevel();
        BlockState blockState = level.getBlockState(blockPos);
        int signal = blockState.getSignal(level, blockPos, direction);
        if (!levelPredicate.test(signal)) {
            throw this.assertionException(pos, errorMessage.get());
        }
    }

    public void assertEntityPresent(final EntityType<?> entityType) {
        if (!this.getLevel().hasEntities(entityType, this.getBounds(), Entity::isAlive)) {
            throw this.assertionException("test.error.expected_entity_in_test", entityType.getDescription());
        }
    }

    public void assertEntityPresent(final EntityType<?> entityType, final int x, final int y, final int z) {
        this.assertEntityPresent(entityType, new BlockPos(x, y, z));
    }

    public void assertEntityPresent(final EntityType<?> entityType, final BlockPos pos) {
        BlockPos absolutePos = this.absolutePos(pos);
        if (!this.getLevel().hasEntities(entityType, new AABB(absolutePos), Entity::isAlive)) {
            throw this.assertionException(pos, "test.error.expected_entity", entityType.getDescription());
        }
    }

    public void assertEntityPresent(final EntityType<?> entityType, final AABB relativeAABB) {
        AABB absoluteAABB = this.absoluteAABB(relativeAABB);
        if (!this.getLevel().hasEntities(entityType, absoluteAABB, Entity::isAlive)) {
            throw this.assertionException(BlockPos.containing(relativeAABB.getCenter()), "test.error.expected_entity", entityType.getDescription());
        }
    }

    public void assertEntityPresent(final EntityType<?> entityType, final AABB relativeAABB, final Component message) {
        AABB absoluteAABB = this.absoluteAABB(relativeAABB);
        if (!this.getLevel().hasEntities(entityType, absoluteAABB, Entity::isAlive)) {
            throw this.assertionException(BlockPos.containing(relativeAABB.getCenter()), message);
        }
    }

    public void assertEntitiesPresent(final EntityType<?> entityType, final int expectedEntities) {
        List<? extends Entity> entities = this.getLevel().getEntities(entityType, this.getBounds(), Entity::isAlive);
        if (entities.size() != expectedEntities) {
            throw this.assertionException("test.error.expected_entity_count", expectedEntities, entityType.getDescription(), entities.size());
        }
    }

    public void assertEntitiesPresent(final EntityType<?> entityType, final BlockPos pos, final int numOfExpectedEntities, final double distance) {
        BlockPos absolutePos = this.absolutePos(pos);
        List<? extends Entity> entities = this.getEntities((EntityType<? extends Entity>)entityType, pos, distance);
        if (entities.size() != numOfExpectedEntities) {
            throw this.assertionException(pos, "test.error.expected_entity_count", numOfExpectedEntities, entityType.getDescription(), entities.size());
        }
    }

    public void assertEntityPresent(final EntityType<?> entityType, final BlockPos pos, final double distance) {
        List<? extends Entity> entities = this.getEntities((EntityType<? extends Entity>)entityType, pos, distance);
        if (entities.isEmpty()) {
            BlockPos absolutePos = this.absolutePos(pos);
            throw this.assertionException(pos, "test.error.expected_entity", entityType.getDescription());
        }
    }

    public <T extends Entity> List<T> getEntities(final EntityType<T> entityType, final BlockPos pos, final double distance) {
        BlockPos absolutePos = this.absolutePos(pos);
        return this.getLevel().getEntities(entityType, new AABB(absolutePos).inflate(distance), Entity::isAlive);
    }

    public <T extends Entity> List<T> getEntities(final EntityType<T> entityType) {
        return this.getLevel().getEntities(entityType, this.getBounds(), Entity::isAlive);
    }

    public void assertEntityInstancePresent(final Entity entity, final int x, final int y, final int z) {
        this.assertEntityInstancePresent(entity, new BlockPos(x, y, z));
    }

    public void assertEntityInstancePresent(final Entity entity, final BlockPos pos) {
        this.assertEntityInstancePresent(entity, pos, 0.0);
    }

    public void assertEntityInstancePresent(final Entity entity, final BlockPos pos, final double inflate) {
        BlockPos absolutePos = this.absolutePos(pos);
        List<? extends Entity> entities = this.getLevel().getEntities(entity.getType(), new AABB(absolutePos).inflate(inflate), Entity::isAlive);
        entities.stream()
            .filter(it -> it == entity)
            .findFirst()
            .orElseThrow(() -> this.assertionException(pos, "test.error.expected_entity", entity.getType().getDescription()));
    }

    public void assertItemEntityCountIs(final Item itemType, final BlockPos pos, final double distance, final int count) {
        BlockPos absolutePos = this.absolutePos(pos);
        List<ItemEntity> entities = this.getLevel().getEntities(EntityTypes.ITEM, new AABB(absolutePos).inflate(distance), Entity::isAlive);
        int num = 0;

        for (ItemEntity entity : entities) {
            ItemStack itemStack = entity.getItem();
            if (itemStack.is(itemType)) {
                num += itemStack.getCount();
            }
        }

        if (num != count) {
            throw this.assertionException(pos, "test.error.expected_items_count", count, getItemName(itemType), num);
        }
    }

    public void assertItemEntityPresent(final Item itemType, final BlockPos pos, final double distance) {
        BlockPos absolutePos = this.absolutePos(pos);
        Predicate<ItemEntity> isSameItem = entity -> entity.isAlive() && entity.getItem().is(itemType);
        if (!this.getLevel().hasEntities(EntityTypes.ITEM, new AABB(absolutePos).inflate(distance), isSameItem)) {
            throw this.assertionException(pos, "test.error.expected_item", getItemName(itemType));
        }
    }

    public void assertItemEntityNotPresent(final Item itemType, final BlockPos pos, final double distance) {
        BlockPos absolutePos = this.absolutePos(pos);
        Predicate<ItemEntity> isSameItem = entity -> entity.isAlive() && entity.getItem().is(itemType);
        if (this.getLevel().hasEntities(EntityTypes.ITEM, new AABB(absolutePos).inflate(distance), isSameItem)) {
            throw this.assertionException(pos, "test.error.unexpected_item", getItemName(itemType));
        }
    }

    public void assertItemEntityPresent(final Item itemType) {
        Predicate<ItemEntity> isSameItem = entity -> entity.isAlive() && entity.getItem().is(itemType);
        if (!this.getLevel().hasEntities(EntityTypes.ITEM, this.getBounds(), isSameItem)) {
            throw this.assertionException("test.error.expected_item", getItemName(itemType));
        }
    }

    public void assertItemEntityNotPresent(final Item itemType) {
        Predicate<ItemEntity> isSameItem = entity -> entity.isAlive() && entity.getItem().is(itemType);
        if (this.getLevel().hasEntities(EntityTypes.ITEM, this.getBounds(), isSameItem)) {
            throw this.assertionException("test.error.unexpected_item", getItemName(itemType));
        }
    }

    public void assertEntityNotPresent(final EntityType<?> entityType) {
        List<? extends Entity> entities = this.getLevel().getEntities(entityType, this.getBounds(), Entity::isAlive);
        if (!entities.isEmpty()) {
            throw this.assertionException(entities.getFirst().blockPosition(), "test.error.unexpected_entity", entityType.getDescription());
        }
    }

    public void assertEntityNotPresent(final EntityType<?> entityType, final int x, final int y, final int z) {
        this.assertEntityNotPresent(entityType, new BlockPos(x, y, z));
    }

    public void assertEntityNotPresent(final EntityType<?> entityType, final BlockPos pos) {
        BlockPos absolutePos = this.absolutePos(pos);
        if (this.getLevel().hasEntities(entityType, new AABB(absolutePos), Entity::isAlive)) {
            throw this.assertionException(pos, "test.error.unexpected_entity", entityType.getDescription());
        }
    }

    public void assertEntityNotPresent(final EntityType<?> entityType, final AABB relativeAABB) {
        AABB absoluteAABB = this.absoluteAABB(relativeAABB);
        List<? extends Entity> entities = this.getLevel().getEntities(entityType, absoluteAABB, Entity::isAlive);
        if (!entities.isEmpty()) {
            throw this.assertionException(entities.getFirst().blockPosition(), "test.error.unexpected_entity", entityType.getDescription());
        }
    }

    public void assertEntityTouching(final EntityType<?> entityType, final double x, final double y, final double z) {
        Vec3 vec = new Vec3(x, y, z);
        Vec3 absoluteVec = this.absoluteVec(vec);
        Predicate<? super Entity> predicate = e -> e.getBoundingBox().intersects(absoluteVec, absoluteVec);
        if (!this.getLevel().hasEntities(entityType, this.getBounds(), predicate)) {
            throw this.assertionException(
                "test.error.expected_entity_touching", entityType.getDescription(), absoluteVec.x(), absoluteVec.y(), absoluteVec.z(), x, y, z
            );
        }
    }

    public void assertEntityNotTouching(final EntityType<?> entityType, final double x, final double y, final double z) {
        Vec3 vec = new Vec3(x, y, z);
        Vec3 absoluteVec = this.absoluteVec(vec);
        Predicate<? super Entity> predicate = e -> !e.getBoundingBox().intersects(absoluteVec, absoluteVec);
        if (!this.getLevel().hasEntities(entityType, this.getBounds(), predicate)) {
            throw this.assertionException(
                "test.error.expected_entity_not_touching", entityType.getDescription(), absoluteVec.x(), absoluteVec.y(), absoluteVec.z(), x, y, z
            );
        }
    }

    public <E extends Entity, T> void assertEntityData(final BlockPos pos, final EntityType<E> entityType, final Predicate<E> test) {
        BlockPos absolutePos = this.absolutePos(pos);
        List<E> entities = this.getLevel().getEntities(entityType, new AABB(absolutePos), Entity::isAlive);
        if (entities.isEmpty()) {
            throw this.assertionException(pos, "test.error.expected_entity", entityType.getDescription());
        }

        for (E entity : entities) {
            if (!test.test(entity)) {
                throw this.assertionException(entity.blockPosition(), "test.error.expected_entity_data_predicate", entity.getName());
            }
        }
    }

    public <E extends Entity, T> void assertEntityData(
        final BlockPos pos, final EntityType<E> entityType, final Function<? super E, T> dataAccessor, final @Nullable T data
    ) {
        this.assertEntityData(new AABB(pos), entityType, dataAccessor, data);
    }

    public <E extends Entity, T> void assertEntityData(
        final AABB box, final EntityType<E> entityType, final Function<? super E, T> dataAccessor, final @Nullable T data
    ) {
        List<E> entities = this.getLevel().getEntities(entityType, this.absoluteAABB(box), Entity::isAlive);
        if (entities.isEmpty()) {
            throw this.assertionException(BlockPos.containing(box.getBottomCenter()), "test.error.expected_entity", entityType.getDescription());
        }

        for (E entity : entities) {
            T actual = dataAccessor.apply(entity);
            if (!Objects.equals(actual, data)) {
                throw this.assertionException(BlockPos.containing(box.getBottomCenter()), "test.error.expected_entity_data", data, actual);
            }
        }
    }

    public <E extends LivingEntity> void assertEntityIsHolding(final BlockPos pos, final EntityType<E> entityType, final Item item) {
        BlockPos absolutePos = this.absolutePos(pos);
        List<E> entities = this.getLevel().getEntities(entityType, new AABB(absolutePos), Entity::isAlive);
        if (entities.isEmpty()) {
            throw this.assertionException(pos, "test.error.expected_entity", entityType.getDescription());
        }

        for (E entity : entities) {
            if (entity.isHolding(item)) {
                return;
            }
        }

        throw this.assertionException(pos, "test.error.expected_entity_holding", getItemName(item));
    }

    public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(final BlockPos pos, final EntityType<E> entityType, final Item item) {
        BlockPos absolutePos = this.absolutePos(pos);
        List<E> entities = this.getLevel().getEntities(entityType, new AABB(absolutePos), rec$ -> rec$.isAlive());
        if (entities.isEmpty()) {
            throw this.assertionException(pos, "test.error.expected_entity", entityType.getDescription());
        }

        for (E entity : entities) {
            if (entity.getInventory().hasAnyMatching(itemStack -> itemStack.is(item))) {
                return;
            }
        }

        throw this.assertionException(pos, "test.error.expected_entity_having", getItemName(item));
    }

    public void assertContainerEmpty(final BlockPos pos) {
        BaseContainerBlockEntity container = this.getBlockEntity(pos, BaseContainerBlockEntity.class);
        if (!container.isEmpty()) {
            throw this.assertionException(pos, "test.error.expected_empty_container");
        }
    }

    public void assertContainerContainsSingle(final BlockPos pos, final Item item) {
        BaseContainerBlockEntity container = this.getBlockEntity(pos, BaseContainerBlockEntity.class);
        if (container.countItem(item) != 1) {
            throw this.assertionException(pos, "test.error.expected_container_contents_single", getItemName(item));
        }
    }

    public void assertContainerContains(final BlockPos pos, final Item item) {
        BaseContainerBlockEntity container = this.getBlockEntity(pos, BaseContainerBlockEntity.class);
        if (container.countItem(item) == 0) {
            throw this.assertionException(pos, "test.error.expected_container_contents", getItemName(item));
        }
    }

    public void assertSameBlockStates(final BoundingBox sourceBoundingBox, final BlockPos targetBoundingBoxCorner) {
        BlockPos.betweenClosedStream(sourceBoundingBox)
            .forEach(
                sourcePos -> {
                    BlockPos targetPos = targetBoundingBoxCorner.offset(
                        sourcePos.getX() - sourceBoundingBox.minX(), sourcePos.getY() - sourceBoundingBox.minY(), sourcePos.getZ() - sourceBoundingBox.minZ()
                    );
                    this.assertSameBlockState(sourcePos, targetPos);
                }
            );
    }

    public void assertSameBlockState(final BlockPos sourcePos, final BlockPos targetPos) {
        BlockState sourceState = this.getBlockState(sourcePos);
        BlockState targetState = this.getBlockState(targetPos);
        if (sourceState != targetState) {
            throw this.assertionException(sourcePos, "test.error.state_not_equal", targetState, sourceState);
        }
    }

    public void assertAtTickTimeContainerContains(final long time, final BlockPos pos, final Item item) {
        this.runAtTickTime(time, () -> this.assertContainerContainsSingle(pos, item));
    }

    public void assertAtTickTimeContainerEmpty(final long time, final BlockPos pos) {
        this.runAtTickTime(time, () -> this.assertContainerEmpty(pos));
    }

    public <E extends Entity, T> void succeedWhenEntityData(final BlockPos pos, final EntityType<E> entityType, final Function<E, T> dataAccessor, final T data) {
        this.succeedWhen(() -> this.assertEntityData(pos, entityType, dataAccessor, data));
    }

    public <E extends Entity> void assertEntityProperty(final E entity, final Predicate<E> test, final Component description) {
        if (!test.test(entity)) {
            throw this.assertionException(entity.blockPosition(), "test.error.entity_property", entity.getName(), description);
        }
    }

    public <E extends Entity, T> void assertEntityProperty(final E entity, final Function<E, T> test, final T expected, final Component description) {
        T actual = test.apply(entity);
        if (!actual.equals(expected)) {
            throw this.assertionException(entity.blockPosition(), "test.error.entity_property_details", entity.getName(), description, actual, expected);
        }
    }

    public void assertLivingEntityHasMobEffect(final LivingEntity entity, final Holder<MobEffect> mobEffect, final int amplifier) {
        MobEffectInstance mobEffectInstance = entity.getEffect(mobEffect);
        if (mobEffectInstance == null || mobEffectInstance.getAmplifier() != amplifier) {
            throw this.assertionException("test.error.expected_entity_effect", entity.getName(), PotionContents.getPotionDescription(mobEffect, amplifier));
        }
    }

    public void succeedWhenEntityPresent(final EntityType<?> entityType, final int x, final int y, final int z) {
        this.succeedWhenEntityPresent(entityType, new BlockPos(x, y, z));
    }

    public void succeedWhenEntityPresent(final EntityType<?> entityType, final BlockPos pos) {
        this.succeedWhen(() -> this.assertEntityPresent(entityType, pos));
    }

    public void succeedWhenEntityNotPresent(final EntityType<?> entityType, final int x, final int y, final int z) {
        this.succeedWhenEntityNotPresent(entityType, new BlockPos(x, y, z));
    }

    public void succeedWhenEntityNotPresent(final EntityType<?> entityType, final BlockPos pos) {
        this.succeedWhen(() -> this.assertEntityNotPresent(entityType, pos));
    }

    public void succeed() {
        this.testInfo.succeed();
    }

    private void ensureSingleFinalCheck() {
        if (this.finalCheckAdded) {
            throw new IllegalStateException("This test already has final clause");
        }

        this.finalCheckAdded = true;
    }

    public void succeedIf(final Runnable asserter) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(0L, asserter).thenSucceed();
    }

    public void succeedWhen(final Runnable asserter) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(asserter).thenSucceed();
    }

    public void succeedOnTickWhen(final int tick, final Runnable asserter) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(tick, asserter).thenSucceed();
    }

    public void runAtTickTime(final long time, final Runnable asserter) {
        this.testInfo.setRunAtTickTime(time, asserter);
    }

    public void runBeforeTestEnd(final Runnable asserter) {
        this.runAtTickTime(this.testInfo.getTimeoutTicks() - 1, asserter);
    }

    public void runAfterDelay(final long ticksToDelay, final Runnable whatToRun) {
        this.runAtTickTime(this.testInfo.getTick() + ticksToDelay, whatToRun);
    }

    public void setTime(final long ticks) {
        ServerLevel level = this.getLevel();
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        level.clockManager().setTotalTicks(clock, ticks);
    }

    public void randomTick(final BlockPos pos) {
        BlockPos absolutePos = this.absolutePos(pos);
        ServerLevel level = this.getLevel();
        level.getBlockState(absolutePos).randomTick(level, absolutePos, level.getRandom());
    }

    public void tickBlock(final BlockPos pos) {
        BlockPos absolutePos = this.absolutePos(pos);
        ServerLevel level = this.getLevel();
        level.getBlockState(absolutePos).tick(level, absolutePos, level.getRandom());
    }

    public void tickPrecipitation(final BlockPos pos) {
        BlockPos absolutePos = this.absolutePos(pos);
        ServerLevel level = this.getLevel();
        level.tickPrecipitation(absolutePos);
    }

    public void tickPrecipitation() {
        AABB aabb = this.getRelativeBounds();
        int maxX = (int)Math.floor(aabb.maxX);
        int maxZ = (int)Math.floor(aabb.maxZ);
        int maxY = (int)Math.floor(aabb.maxY);

        for (int x = (int)Math.floor(aabb.minX); x < maxX; x++) {
            for (int z = (int)Math.floor(aabb.minZ); z < maxZ; z++) {
                this.tickPrecipitation(new BlockPos(x, maxY, z));
            }
        }
    }

    public int getHeight(final Heightmap.Types heightmap, final int x, final int z) {
        BlockPos absolutePos = this.absolutePos(new BlockPos(x, 0, z));
        return this.relativePos(this.getLevel().getHeightmapPos(heightmap, absolutePos)).getY();
    }

    public void fail(final Component message, final BlockPos pos) {
        throw this.assertionException(pos, message);
    }

    public void fail(final Component message, final Entity entity) {
        throw this.assertionException(entity.blockPosition(), message);
    }

    public void fail(final Component message) {
        throw this.assertionException(message);
    }

    public void fail(final String message) {
        throw this.assertionException(Component.literal(message));
    }

    public void failIf(final Runnable asserter) {
        this.testInfo.createSequence().thenWaitUntil(asserter).thenFail(() -> this.assertionException("test.error.fail"));
    }

    public void failIfEver(final Runnable asserter) {
        LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks()).forEach(i -> this.testInfo.setRunAtTickTime(i, asserter::run));
    }

    public GameTestSequence startSequence() {
        return this.testInfo.createSequence();
    }

    public BlockPos absolutePos(final BlockPos relativePos) {
        BlockPos testPos = this.testInfo.getTestOrigin();
        BlockPos absolutePosBeforeTransform = testPos.offset(relativePos);
        return StructureTemplate.transform(absolutePosBeforeTransform, Mirror.NONE, this.testInfo.getRotation(), testPos);
    }

    public BlockPos relativePos(final BlockPos absolutePos) {
        BlockPos testPos = this.testInfo.getTestOrigin();
        Rotation inverseRotation = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
        BlockPos absolutePosBeforeTransform = StructureTemplate.transform(absolutePos, Mirror.NONE, inverseRotation, testPos);
        return absolutePosBeforeTransform.subtract(testPos);
    }

    public AABB absoluteAABB(final AABB relativeAABB) {
        Vec3 min = this.absoluteVec(relativeAABB.getMinPosition());
        Vec3 max = this.absoluteVec(relativeAABB.getMaxPosition());
        return new AABB(min, max);
    }

    public AABB relativeAABB(final AABB absoluteAABB) {
        Vec3 min = this.relativeVec(absoluteAABB.getMinPosition());
        Vec3 max = this.relativeVec(absoluteAABB.getMaxPosition());
        return new AABB(min, max);
    }

    public Vec3 absoluteVec(final Vec3 relativeVec) {
        Vec3 testPosVec = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
        return StructureTemplate.transform(testPosVec.add(relativeVec), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
    }

    public Vec3 relativeVec(final Vec3 absoluteVec) {
        Vec3 testPosVec = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
        return StructureTemplate.transform(absoluteVec.subtract(testPosVec), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
    }

    public Rotation getTestRotation() {
        return this.testInfo.getRotation();
    }

    public Direction getTestDirection() {
        return this.testInfo.getRotation().rotate(Direction.SOUTH);
    }

    public Direction getAbsoluteDirection(final Direction direction) {
        return this.getTestRotation().rotate(direction);
    }

    public void assertTrue(final boolean condition, final Component errorMessage) {
        if (!condition) {
            throw this.assertionException(errorMessage);
        }
    }

    public void assertTrue(final boolean condition, final String errorMessage) {
        this.assertTrue(condition, Component.literal(errorMessage));
    }

    public <N> void assertValueEqual(final N value, final N expected, final String valueName) {
        this.assertValueEqual(value, expected, Component.literal(valueName));
    }

    public <N> void assertValueEqual(final N value, final N expected, final Component valueName) {
        if (!value.equals(expected)) {
            throw this.assertionException("test.error.value_not_equal", valueName, expected, value);
        }
    }

    public <N extends Comparable<N>> void assertValueInBetween(final N lowerBound, final N value, final N upperBound, final String valueName) {
        this.assertValueInBetween(lowerBound, value, upperBound, Component.literal(valueName));
    }

    public <N extends Comparable<N>> void assertValueInBetween(final N lowerBound, final N value, final N upperBound, final Component valueName) {
        if (value.compareTo(lowerBound) < 0 || value.compareTo(upperBound) > 0) {
            throw this.assertionException("test.error.value_not_in_between", valueName, lowerBound, upperBound, value);
        }
    }

    public void assertFalse(final boolean condition, final Component errorMessage) {
        this.assertTrue(!condition, errorMessage);
    }

    public void assertFalse(final boolean condition, final String errorMessage) {
        this.assertFalse(condition, Component.literal(errorMessage));
    }

    public long getTick() {
        return this.testInfo.getTick();
    }

    public AABB getBounds() {
        return this.testInfo.getStructureBounds();
    }

    public AABB getBoundsWithPadding() {
        return this.getBounds().inflate(this.testInfo.getTest().padding());
    }

    public AABB getRelativeBounds() {
        AABB absolute = this.testInfo.getStructureBounds();
        Rotation rotation = this.testInfo.getRotation();

        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> new AABB(0.0, 0.0, 0.0, absolute.getZsize(), absolute.getYsize(), absolute.getXsize());
            default -> new AABB(0.0, 0.0, 0.0, absolute.getXsize(), absolute.getYsize(), absolute.getZsize());
        };
    }

    public void forEveryBlockInStructure(final Consumer<BlockPos> forBlock) {
        AABB aabb = this.getRelativeBounds().contract(1.0, 1.0, 1.0);
        BlockPos.MutableBlockPos.betweenClosedStream(aabb).forEach(forBlock);
    }

    public void onEachTick(final Runnable action) {
        LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks()).forEach(i -> this.testInfo.setRunAtTickTime(i, action::run));
    }

    public void placeAt(final Player player, final ItemStack blockStack, final BlockPos pos, final Direction face) {
        BlockPos absolute = this.absolutePos(pos.relative(face));
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absolute), face, absolute, false);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult);
        blockStack.useOn(context);
    }

    public void setBiome(final ResourceKey<Biome> biome) {
        AABB bounds = this.getBoundsWithPadding();
        BlockPos low = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos high = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        Either<Integer, CommandSyntaxException> result = FillBiomeCommand.fill(
            this.getLevel(), low, high, this.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome)
        );
        if (result.right().isPresent()) {
            throw this.assertionException("test.error.set_biome");
        }
    }

    private static Component getItemName(final Item itemType) {
        return itemType.components().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
    }

    private class TestBlockPlaceContext extends BlockPlaceContext {
        private final Direction placeDirection;

        public TestBlockPlaceContext(
            final Level level, final InteractionHand hand, final ItemStack itemStackInHand, final BlockHitResult hitResult, final Direction placeDirection
        ) {
            super(level, null, hand, itemStackInHand, hitResult);
            this.placeDirection = placeDirection;
        }

        @Override
        public Direction getNearestLookingDirection() {
            return this.placeDirection;
        }

        @Override
        public Direction[] getNearestLookingDirections() {
            Direction[] directions = Direction.values();
            Arrays.sort(directions, Comparator.comparingDouble(d -> d.getUnitVec3().distanceTo(this.placeDirection.getUnitVec3())));
            return directions;
        }
    }
}