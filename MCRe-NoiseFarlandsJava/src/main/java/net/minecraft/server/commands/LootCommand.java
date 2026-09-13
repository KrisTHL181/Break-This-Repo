package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class LootCommand {
    private static final DynamicCommandExceptionType ERROR_NO_HELD_ITEMS = new DynamicCommandExceptionType(
        entity -> Component.translatableEscape("commands.drop.no_held_items", entity)
    );
    private static final DynamicCommandExceptionType ERROR_NO_ENTITY_LOOT_TABLE = new DynamicCommandExceptionType(
        entity -> Component.translatableEscape("commands.drop.no_loot_table.entity", entity)
    );
    private static final DynamicCommandExceptionType ERROR_NO_BLOCK_LOOT_TABLE = new DynamicCommandExceptionType(
        block -> Component.translatableEscape("commands.drop.no_loot_table.block", block)
    );

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            addTargets(
                Commands.literal("loot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)),
                (target, output) -> target.then(
                        Commands.literal("fish")
                            .then(
                                Commands.argument("loot_table", ResourceOrIdArgument.lootTable(context))
                                    .then(
                                        Commands.argument("pos", BlockPosArgument.blockPos())
                                            .executes(
                                                c -> dropFishingLoot(
                                                    c,
                                                    ResourceOrIdArgument.getLootTable(c, "loot_table"),
                                                    BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                    ItemStack.EMPTY,
                                                    output
                                                )
                                            )
                                            .then(
                                                Commands.argument("tool", ItemArgument.item(context))
                                                    .executes(
                                                        c -> dropFishingLoot(
                                                            c,
                                                            ResourceOrIdArgument.getLootTable(c, "loot_table"),
                                                            BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                            ItemArgument.getItem(c, "tool").createItemStack(1),
                                                            output
                                                        )
                                                    )
                                            )
                                            .then(
                                                Commands.literal("mainhand")
                                                    .executes(
                                                        c -> dropFishingLoot(
                                                            c,
                                                            ResourceOrIdArgument.getLootTable(c, "loot_table"),
                                                            BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                            getSourceHandItem(c.getSource(), EquipmentSlot.MAINHAND),
                                                            output
                                                        )
                                                    )
                                            )
                                            .then(
                                                Commands.literal("offhand")
                                                    .executes(
                                                        c -> dropFishingLoot(
                                                            c,
                                                            ResourceOrIdArgument.getLootTable(c, "loot_table"),
                                                            BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                            getSourceHandItem(c.getSource(), EquipmentSlot.OFFHAND),
                                                            output
                                                        )
                                                    )
                                            )
                                    )
                            )
                    )
                    .then(
                        Commands.literal("loot")
                            .then(
                                Commands.argument("loot_table", ResourceOrIdArgument.lootTable(context))
                                    .executes(c -> dropChestLoot(c, ResourceOrIdArgument.getLootTable(c, "loot_table"), output))
                            )
                    )
                    .then(
                        Commands.literal("kill")
                            .then(
                                Commands.argument("target", EntityArgument.entity())
                                    .executes(c -> dropKillLoot(c, EntityArgument.getEntity(c, "target"), output))
                            )
                    )
                    .then(
                        Commands.literal("mine")
                            .then(
                                Commands.argument("pos", BlockPosArgument.blockPos())
                                    .executes(c -> dropBlockLoot(c, BlockPosArgument.getLoadedBlockPos(c, "pos"), ItemStack.EMPTY, output))
                                    .then(
                                        Commands.argument("tool", ItemArgument.item(context))
                                            .executes(
                                                c -> dropBlockLoot(
                                                    c, BlockPosArgument.getLoadedBlockPos(c, "pos"), ItemArgument.getItem(c, "tool").createItemStack(1), output
                                                )
                                            )
                                    )
                                    .then(
                                        Commands.literal("mainhand")
                                            .executes(
                                                c -> dropBlockLoot(
                                                    c,
                                                    BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                    getSourceHandItem(c.getSource(), EquipmentSlot.MAINHAND),
                                                    output
                                                )
                                            )
                                    )
                                    .then(
                                        Commands.literal("offhand")
                                            .executes(
                                                c -> dropBlockLoot(
                                                    c,
                                                    BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                    getSourceHandItem(c.getSource(), EquipmentSlot.OFFHAND),
                                                    output
                                                )
                                            )
                                    )
                            )
                    )
            )
        );
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addTargets(final T root, final LootCommand.TailProvider tail) {
        return root.then(
                Commands.literal("replace")
                    .then(
                        Commands.literal("entity")
                            .then(
                                Commands.argument("entities", EntityArgument.entities())
                                    .then(
                                        tail.construct(
                                                Commands.argument("slot", SlotArgument.slot()),
                                                (c, drops, callback) -> entityReplace(
                                                    EntityArgument.getEntities(c, "entities"), SlotArgument.getSlot(c, "slot"), drops.size(), drops, callback
                                                )
                                            )
                                            .then(
                                                tail.construct(
                                                    Commands.argument("count", IntegerArgumentType.integer(0)),
                                                    (c, drops, callback) -> entityReplace(
                                                        EntityArgument.getEntities(c, "entities"),
                                                        SlotArgument.getSlot(c, "slot"),
                                                        IntegerArgumentType.getInteger(c, "count"),
                                                        drops,
                                                        callback
                                                    )
                                                )
                                            )
                                    )
                            )
                    )
                    .then(
                        Commands.literal("block")
                            .then(
                                Commands.argument("targetPos", BlockPosArgument.blockPos())
                                    .then(
                                        tail.construct(
                                                Commands.argument("slot", SlotArgument.slot()),
                                                (c, drops, callback) -> blockReplace(
                                                    c.getSource(),
                                                    BlockPosArgument.getLoadedBlockPos(c, "targetPos"),
                                                    SlotArgument.getSlot(c, "slot"),
                                                    drops.size(),
                                                    drops,
                                                    callback
                                                )
                                            )
                                            .then(
                                                tail.construct(
                                                    Commands.argument("count", IntegerArgumentType.integer(0)),
                                                    (c, drops, callback) -> blockReplace(
                                                        c.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(c, "targetPos"),
                                                        IntegerArgumentType.getInteger(c, "slot"),
                                                        IntegerArgumentType.getInteger(c, "count"),
                                                        drops,
                                                        callback
                                                    )
                                                )
                                            )
                                    )
                            )
                    )
            )
            .then(
                Commands.literal("insert")
                    .then(
                        tail.construct(
                            Commands.argument("targetPos", BlockPosArgument.blockPos()),
                            (c, drops, callback) -> blockDistribute(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "targetPos"), drops, callback)
                        )
                    )
            )
            .then(
                Commands.literal("give")
                    .then(
                        tail.construct(
                            Commands.argument("players", EntityArgument.players()),
                            (c, drops, callback) -> playerGive(EntityArgument.getPlayers(c, "players"), drops, callback)
                        )
                    )
            )
            .then(
                Commands.literal("spawn")
                    .then(
                        tail.construct(
                            Commands.argument("targetPos", Vec3Argument.vec3()),
                            (c, drops, callback) -> dropInWorld(c.getSource(), Vec3Argument.getVec3(c, "targetPos"), drops, callback)
                        )
                    )
            );
    }

    private static Container getContainer(final CommandSourceStack source, final BlockPos pos) throws CommandSyntaxException {
        if (source.getLevel().getBlockEntity(pos) instanceof Container container) {
            return container;
        } else {
            throw ItemCommands.ERROR_TARGET_NOT_A_CONTAINER.create(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private static int blockDistribute(final CommandSourceStack source, final BlockPos pos, final List<ItemStack> drops, final LootCommand.Callback callback) throws CommandSyntaxException {
        Container container = getContainer(source, pos);
        List<ItemStack> usedItems = Lists.newArrayListWithCapacity(drops.size());

        for (ItemStack drop : drops) {
            if (distributeToContainer(container, drop.copy())) {
                container.setChanged();
                usedItems.add(drop);
            }
        }

        callback.accept(usedItems);
        return usedItems.size();
    }

    private static boolean distributeToContainer(final Container container, final ItemStack itemStack) {
        boolean changed = false;

        for (int slot = 0; slot < container.getContainerSize() && !itemStack.isEmpty(); slot++) {
            ItemStack current = container.getItem(slot);
            if (container.canPlaceItem(slot, itemStack)) {
                if (current.isEmpty()) {
                    container.setItem(slot, itemStack);
                    changed = true;
                    break;
                }

                if (canMergeItems(current, itemStack)) {
                    int space = itemStack.getMaxStackSize() - current.getCount();
                    int count = Math.min(itemStack.getCount(), space);
                    itemStack.shrink(count);
                    current.grow(count);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private static int blockReplace(
        final CommandSourceStack source,
        final BlockPos pos,
        final int startSlot,
        final int slotCount,
        final List<ItemStack> drops,
        final LootCommand.Callback callback
    ) throws CommandSyntaxException {
        Container container = getContainer(source, pos);
        int maxSlot = container.getContainerSize();
        if (startSlot >= 0 && startSlot < maxSlot) {
            List<ItemStack> usedItems = Lists.newArrayListWithCapacity(drops.size());

            for (int i = 0; i < slotCount; i++) {
                int slot = startSlot + i;
                ItemStack toAdd = i < drops.size() ? drops.get(i) : ItemStack.EMPTY;
                if (container.canPlaceItem(slot, toAdd)) {
                    container.setItem(slot, toAdd);
                    usedItems.add(toAdd);
                }
            }

            callback.accept(usedItems);
            return usedItems.size();
        } else {
            throw ItemCommands.ERROR_TARGET_INAPPLICABLE_SLOT.create(startSlot);
        }
    }

    private static boolean canMergeItems(final ItemStack a, final ItemStack b) {
        return a.getCount() <= a.getMaxStackSize() && ItemStack.isSameItemSameComponents(a, b);
    }

    private static int playerGive(final Collection<ServerPlayer> players, final List<ItemStack> drops, final LootCommand.Callback callback) throws CommandSyntaxException {
        List<ItemStack> usedItems = Lists.newArrayListWithCapacity(drops.size());

        for (ItemStack drop : drops) {
            for (ServerPlayer player : players) {
                if (player.getInventory().add(drop.copy())) {
                    usedItems.add(drop);
                }
            }
        }

        callback.accept(usedItems);
        return usedItems.size();
    }

    private static void setSlots(final Entity entity, final List<ItemStack> itemsToSet, final int startSlot, final int count, final List<ItemStack> usedItems) {
        for (int i = 0; i < count; i++) {
            ItemStack item = i < itemsToSet.size() ? itemsToSet.get(i) : ItemStack.EMPTY;
            SlotAccess slotAccess = entity.getSlot(startSlot + i);
            if (slotAccess != null && slotAccess.set(item.copy())) {
                usedItems.add(item);
            }
        }
    }

    private static int entityReplace(
        final Collection<? extends Entity> entities, final int startSlot, final int count, final List<ItemStack> drops, final LootCommand.Callback callback
    ) throws CommandSyntaxException {
        List<ItemStack> usedItems = Lists.newArrayListWithCapacity(drops.size());

        for (Entity entity : entities) {
            if (entity instanceof ServerPlayer player) {
                setSlots(entity, drops, startSlot, count, usedItems);
                player.containerMenu.broadcastChanges();
            } else {
                setSlots(entity, drops, startSlot, count, usedItems);
            }
        }

        callback.accept(usedItems);
        return usedItems.size();
    }

    private static int dropInWorld(final CommandSourceStack source, final Vec3 pos, final List<ItemStack> drops, final LootCommand.Callback callback) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        drops.forEach(drop -> {
            ItemEntity entity = new ItemEntity(level, pos.x, pos.y, pos.z, drop.copy());
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        });
        callback.accept(drops);
        return drops.size();
    }

    private static void callback(final CommandSourceStack source, final List<ItemStack> drops) {
        if (drops.size() == 1) {
            ItemStack drop = drops.get(0);
            source.sendSuccess(() -> Component.translatable("commands.drop.success.single", drop.getCount(), drop.getDisplayName()), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.drop.success.multiple", drops.size()), false);
        }
    }

    private static void callback(final CommandSourceStack source, final List<ItemStack> drops, final ResourceKey<LootTable> location) {
        if (drops.size() == 1) {
            ItemStack drop = drops.get(0);
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.drop.success.single_with_table", drop.getCount(), drop.getDisplayName(), Component.translationArg(location.identifier())
                ),
                false
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.drop.success.multiple_with_table", drops.size(), Component.translationArg(location.identifier())), false
            );
        }
    }

    private static ItemStack getSourceHandItem(final CommandSourceStack source, final EquipmentSlot slot) throws CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getItemBySlot(slot);
        } else {
            throw ERROR_NO_HELD_ITEMS.create(entity.getDisplayName());
        }
    }

    private static int dropBlockLoot(
        final CommandContext<CommandSourceStack> context, final BlockPos pos, final ItemInstance tool, final LootCommand.DropConsumer output
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockState blockState = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        Optional<ResourceKey<LootTable>> lootTable = blockState.getBlock().getLootTable();
        if (lootTable.isEmpty()) {
            throw ERROR_NO_BLOCK_LOOT_TABLE.create(blockState.getBlock().getName());
        }

        LootParams.Builder lootParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.BLOCK_STATE, blockState)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
            .withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity())
            .withParameter(LootContextParams.TOOL, tool);
        List<ItemStack> drops = blockState.getDrops(lootParams);
        return output.accept(context, drops, usedItems -> callback(source, usedItems, lootTable.get()));
    }

    private static int dropKillLoot(final CommandContext<CommandSourceStack> context, final Entity target, final LootCommand.DropConsumer output) throws CommandSyntaxException {
        Optional<ResourceKey<LootTable>> lootTableId = target.getLootTable();
        if (lootTableId.isEmpty()) {
            throw ERROR_NO_ENTITY_LOOT_TABLE.create(target.getDisplayName());
        }

        CommandSourceStack source = context.getSource();
        LootParams.Builder builder = new LootParams.Builder(source.getLevel());
        Entity killer = source.getEntity();
        if (killer instanceof Player player) {
            builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
        }

        builder.withParameter(LootContextParams.DAMAGE_SOURCE, target.damageSources().magic());
        builder.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer);
        builder.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer);
        builder.withParameter(LootContextParams.THIS_ENTITY, target);
        builder.withParameter(LootContextParams.ORIGIN, source.getPosition());
        LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
        LootTable lootTable = source.getServer().reloadableRegistries().getLootTable(lootTableId.get());
        List<ItemStack> drops = lootTable.getRandomItems(lootParams);
        return output.accept(context, drops, usedItems -> callback(source, usedItems, lootTableId.get()));
    }

    private static int dropChestLoot(final CommandContext<CommandSourceStack> context, final Holder<LootTable> lootTable, final LootCommand.DropConsumer output) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        LootParams lootParams = new LootParams.Builder(source.getLevel())
            .withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity())
            .withParameter(LootContextParams.ORIGIN, source.getPosition())
            .create(LootContextParamSets.CHEST);
        return drop(context, lootTable, lootParams, output);
    }

    private static int dropFishingLoot(
        final CommandContext<CommandSourceStack> context,
        final Holder<LootTable> lootTable,
        final BlockPos pos,
        final ItemInstance tool,
        final LootCommand.DropConsumer output
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        LootParams lootParams = new LootParams.Builder(source.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.TOOL, tool)
            .withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity())
            .create(LootContextParamSets.FISHING);
        return drop(context, lootTable, lootParams, output);
    }

    private static int drop(
        final CommandContext<CommandSourceStack> context, final Holder<LootTable> lootTable, final LootParams lootParams, final LootCommand.DropConsumer output
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        List<ItemStack> drops = lootTable.value().getRandomItems(lootParams);
        return output.accept(context, drops, usedItems -> callback(source, usedItems));
    }

    @FunctionalInterface
    private interface Callback {
        void accept(List<ItemStack> setItems) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private interface DropConsumer {
        int accept(CommandContext<CommandSourceStack> context, List<ItemStack> drops, LootCommand.Callback successCallback) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private interface TailProvider {
        ArgumentBuilder<CommandSourceStack, ?> construct(final ArgumentBuilder<CommandSourceStack, ?> root, LootCommand.DropConsumer consumer);
    }
}