package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ItemCommands {
    static final Dynamic3CommandExceptionType ERROR_TARGET_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
        (x, y, z) -> Component.translatableEscape("commands.item.target.not_a_container", x, y, z)
    );
    static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
        (x, y, z) -> Component.translatableEscape("commands.item.source.not_a_container", x, y, z)
    );
    static final DynamicCommandExceptionType ERROR_TARGET_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
        slot -> Component.translatableEscape("commands.item.target.no_such_slot", slot)
    );
    private static final DynamicCommandExceptionType ERROR_SOURCE_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
        slot -> Component.translatableEscape("commands.item.source.no_such_slot", slot)
    );
    private static final DynamicCommandExceptionType ERROR_TARGET_NO_CHANGES = new DynamicCommandExceptionType(
        slot -> Component.translatableEscape("commands.item.target.no_changes", slot)
    );
    private static final Dynamic2CommandExceptionType ERROR_TARGET_NO_CHANGES_KNOWN_ITEM = new Dynamic2CommandExceptionType(
        (item, slot) -> Component.translatableEscape("commands.item.target.no_changed.known_item", item, slot)
    );

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("item")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("replace")
                        .then(
                            Commands.literal("block")
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(
                                            Commands.argument("slot", SlotArgument.slot())
                                                .then(
                                                    Commands.literal("with")
                                                        .then(
                                                            Commands.argument("item", ItemArgument.item(context))
                                                                .executes(
                                                                    c -> setBlockItem(
                                                                        c.getSource(),
                                                                        BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                                        SlotArgument.getSlot(c, "slot"),
                                                                        ItemArgument.getItem(c, "item").createItemStack(1)
                                                                    )
                                                                )
                                                                .then(
                                                                    Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                                        .executes(
                                                                            c -> setBlockItem(
                                                                                c.getSource(),
                                                                                BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                                                SlotArgument.getSlot(c, "slot"),
                                                                                ItemArgument.getItem(c, "item")
                                                                                    .createItemStack(IntegerArgumentType.getInteger(c, "count"))
                                                                            )
                                                                        )
                                                                )
                                                        )
                                                )
                                                .then(
                                                    Commands.literal("from")
                                                        .then(
                                                            Commands.literal("block")
                                                                .then(
                                                                    Commands.argument("source", BlockPosArgument.blockPos())
                                                                        .then(
                                                                            Commands.argument("sourceSlot", SlotArgument.slot())
                                                                                .executes(
                                                                                    c -> blockToBlock(
                                                                                        c.getSource(),
                                                                                        BlockPosArgument.getLoadedBlockPos(c, "source"),
                                                                                        SlotArgument.getSlot(c, "sourceSlot"),
                                                                                        BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                                                        SlotArgument.getSlot(c, "slot")
                                                                                    )
                                                                                )
                                                                                .then(
                                                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context))
                                                                                        .executes(
                                                                                            c -> blockToBlock(
                                                                                                (CommandSourceStack)c.getSource(),
                                                                                                BlockPosArgument.getLoadedBlockPos(c, "source"),
                                                                                                SlotArgument.getSlot(c, "sourceSlot"),
                                                                                                BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                                                                SlotArgument.getSlot(c, "slot"),
                                                                                                ResourceOrIdArgument.getLootModifier(c, "modifier")
                                                                                            )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        .then(
                                                            Commands.literal("entity")
                                                                .then(
                                                                    Commands.argument("source", EntityArgument.entity())
                                                                        .then(
                                                                            Commands.argument("sourceSlot", SlotArgument.slot())
                                                                                .executes(
                                                                                    c -> entityToBlock(
                                                                                        c.getSource(),
                                                                                        EntityArgument.getEntity(c, "source"),
                                                                                        SlotArgument.getSlot(c, "sourceSlot"),
                                                                                        BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                                                        SlotArgument.getSlot(c, "slot")
                                                                                    )
                                                                                )
                                                                                .then(
                                                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context))
                                                                                        .executes(
                                                                                            c -> entityToBlock(
                                                                                                (CommandSourceStack)c.getSource(),
                                                                                                EntityArgument.getEntity(c, "source"),
                                                                                                SlotArgument.getSlot(c, "sourceSlot"),
                                                                                                BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                                                                SlotArgument.getSlot(c, "slot"),
                                                                                                ResourceOrIdArgument.getLootModifier(c, "modifier")
                                                                                            )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("entity")
                                .then(
                                    Commands.argument("targets", EntityArgument.entities())
                                        .then(
                                            Commands.argument("slot", SlotArgument.slot())
                                                .then(
                                                    Commands.literal("with")
                                                        .then(
                                                            Commands.argument("item", ItemArgument.item(context))
                                                                .executes(
                                                                    c -> setEntityItem(
                                                                        c.getSource(),
                                                                        EntityArgument.getEntities(c, "targets"),
                                                                        SlotArgument.getSlot(c, "slot"),
                                                                        ItemArgument.getItem(c, "item").createItemStack(1)
                                                                    )
                                                                )
                                                                .then(
                                                                    Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                                        .executes(
                                                                            c -> setEntityItem(
                                                                                c.getSource(),
                                                                                EntityArgument.getEntities(c, "targets"),
                                                                                SlotArgument.getSlot(c, "slot"),
                                                                                ItemArgument.getItem(c, "item")
                                                                                    .createItemStack(IntegerArgumentType.getInteger(c, "count"))
                                                                            )
                                                                        )
                                                                )
                                                        )
                                                )
                                                .then(
                                                    Commands.literal("from")
                                                        .then(
                                                            Commands.literal("block")
                                                                .then(
                                                                    Commands.argument("source", BlockPosArgument.blockPos())
                                                                        .then(
                                                                            Commands.argument("sourceSlot", SlotArgument.slot())
                                                                                .executes(
                                                                                    c -> blockToEntities(
                                                                                        c.getSource(),
                                                                                        BlockPosArgument.getLoadedBlockPos(c, "source"),
                                                                                        SlotArgument.getSlot(c, "sourceSlot"),
                                                                                        EntityArgument.getEntities(c, "targets"),
                                                                                        SlotArgument.getSlot(c, "slot")
                                                                                    )
                                                                                )
                                                                                .then(
                                                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context))
                                                                                        .executes(
                                                                                            c -> blockToEntities(
                                                                                                (CommandSourceStack)c.getSource(),
                                                                                                BlockPosArgument.getLoadedBlockPos(c, "source"),
                                                                                                SlotArgument.getSlot(c, "sourceSlot"),
                                                                                                EntityArgument.getEntities(c, "targets"),
                                                                                                SlotArgument.getSlot(c, "slot"),
                                                                                                ResourceOrIdArgument.getLootModifier(c, "modifier")
                                                                                            )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        .then(
                                                            Commands.literal("entity")
                                                                .then(
                                                                    Commands.argument("source", EntityArgument.entity())
                                                                        .then(
                                                                            Commands.argument("sourceSlot", SlotArgument.slot())
                                                                                .executes(
                                                                                    c -> entityToEntities(
                                                                                        c.getSource(),
                                                                                        EntityArgument.getEntity(c, "source"),
                                                                                        SlotArgument.getSlot(c, "sourceSlot"),
                                                                                        EntityArgument.getEntities(c, "targets"),
                                                                                        SlotArgument.getSlot(c, "slot")
                                                                                    )
                                                                                )
                                                                                .then(
                                                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context))
                                                                                        .executes(
                                                                                            c -> entityToEntities(
                                                                                                (CommandSourceStack)c.getSource(),
                                                                                                EntityArgument.getEntity(c, "source"),
                                                                                                SlotArgument.getSlot(c, "sourceSlot"),
                                                                                                EntityArgument.getEntities(c, "targets"),
                                                                                                SlotArgument.getSlot(c, "slot"),
                                                                                                ResourceOrIdArgument.getLootModifier(c, "modifier")
                                                                                            )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("modify")
                        .then(
                            Commands.literal("block")
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(
                                            Commands.argument("slot", SlotArgument.slot())
                                                .then(
                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context))
                                                        .executes(
                                                            c -> modifyBlockItem(
                                                                (CommandSourceStack)c.getSource(),
                                                                BlockPosArgument.getLoadedBlockPos(c, "pos"),
                                                                SlotArgument.getSlot(c, "slot"),
                                                                ResourceOrIdArgument.getLootModifier(c, "modifier")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("entity")
                                .then(
                                    Commands.argument("targets", EntityArgument.entities())
                                        .then(
                                            Commands.argument("slot", SlotArgument.slot())
                                                .then(
                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context))
                                                        .executes(
                                                            c -> modifyEntityItem(
                                                                (CommandSourceStack)c.getSource(),
                                                                EntityArgument.getEntities(c, "targets"),
                                                                SlotArgument.getSlot(c, "slot"),
                                                                ResourceOrIdArgument.getLootModifier(c, "modifier")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int modifyBlockItem(final CommandSourceStack source, final BlockPos pos, final int slot, final Holder<LootItemFunction> modifier) throws CommandSyntaxException {
        Container container = getContainer(source, pos, ERROR_TARGET_NOT_A_CONTAINER);
        if (slot >= 0 && slot < container.getContainerSize()) {
            ItemStack itemStack = applyModifier(source, modifier, container.getItem(slot));
            container.setItem(slot, itemStack);
            source.sendSuccess(
                () -> Component.translatable("commands.item.block.set.success", pos.getX(), pos.getY(), pos.getZ(), itemStack.getDisplayName()), true
            );
            return 1;
        } else {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create(slot);
        }
    }

    private static int modifyEntityItem(
        final CommandSourceStack source, final Collection<? extends Entity> entities, final int slot, final Holder<LootItemFunction> modifier
    ) throws CommandSyntaxException {
        Map<Entity, ItemStack> changedEntities = Maps.newHashMapWithExpectedSize(entities.size());

        for (Entity entity : entities) {
            SlotAccess slotAccess = entity.getSlot(slot);
            if (slotAccess != null) {
                ItemStack itemStack = applyModifier(source, modifier, slotAccess.get().copy());
                if (slotAccess.set(itemStack)) {
                    changedEntities.put(entity, itemStack);
                    if (entity instanceof ServerPlayer serverPlayer) {
                        serverPlayer.containerMenu.broadcastChanges();
                    }
                }
            }
        }

        if (changedEntities.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES.create(slot);
        }

        if (changedEntities.size() == 1) {
            Entry<Entity, ItemStack> e = changedEntities.entrySet().iterator().next();
            source.sendSuccess(
                () -> Component.translatable("commands.item.entity.set.success.single", e.getKey().getDisplayName(), e.getValue().getDisplayName()), true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.multiple", changedEntities.size()), true);
        }

        return changedEntities.size();
    }

    private static int setBlockItem(final CommandSourceStack source, final BlockPos pos, final int slot, final ItemStack itemStack) throws CommandSyntaxException {
        Container container = getContainer(source, pos, ERROR_TARGET_NOT_A_CONTAINER);
        if (slot >= 0 && slot < container.getContainerSize()) {
            container.setItem(slot, itemStack);
            source.sendSuccess(
                () -> Component.translatable("commands.item.block.set.success", pos.getX(), pos.getY(), pos.getZ(), itemStack.getDisplayName()), true
            );
            return 1;
        } else {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create(slot);
        }
    }

    static Container getContainer(final CommandSourceStack source, final BlockPos pos, final Dynamic3CommandExceptionType exceptionType) throws CommandSyntaxException {
        if (source.getLevel().getBlockEntity(pos) instanceof Container container) {
            return container;
        } else {
            throw exceptionType.create(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private static int setEntityItem(final CommandSourceStack source, final Collection<? extends Entity> entities, final int slot, final ItemStack itemStack) throws CommandSyntaxException {
        List<Entity> changedEntities = Lists.newArrayListWithCapacity(entities.size());

        for (Entity entity : entities) {
            SlotAccess slotAccess = entity.getSlot(slot);
            if (slotAccess != null && slotAccess.set(itemStack.copy())) {
                changedEntities.add(entity);
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.containerMenu.broadcastChanges();
                }
            }
        }

        if (changedEntities.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES_KNOWN_ITEM.create(itemStack.getDisplayName(), slot);
        }

        if (changedEntities.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable("commands.item.entity.set.success.single", changedEntities.getFirst().getDisplayName(), itemStack.getDisplayName()),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.item.entity.set.success.multiple", changedEntities.size(), itemStack.getDisplayName()), true
            );
        }

        return changedEntities.size();
    }

    private static int blockToEntities(
        final CommandSourceStack source,
        final BlockPos sourcePos,
        final int sourceSlot,
        final Collection<? extends Entity> targetEntities,
        final int targetSlot
    ) throws CommandSyntaxException {
        return setEntityItem(source, targetEntities, targetSlot, getBlockItem(source, sourcePos, sourceSlot));
    }

    private static int blockToEntities(
        final CommandSourceStack source,
        final BlockPos sourcePos,
        final int sourceSlot,
        final Collection<? extends Entity> targetEntities,
        final int targetSlot,
        final Holder<LootItemFunction> modifier
    ) throws CommandSyntaxException {
        return setEntityItem(source, targetEntities, targetSlot, applyModifier(source, modifier, getBlockItem(source, sourcePos, sourceSlot)));
    }

    private static int blockToBlock(
        final CommandSourceStack source, final BlockPos sourcePos, final int sourceSlot, final BlockPos targetPos, final int targetSlot
    ) throws CommandSyntaxException {
        return setBlockItem(source, targetPos, targetSlot, getBlockItem(source, sourcePos, sourceSlot));
    }

    private static int blockToBlock(
        final CommandSourceStack source,
        final BlockPos sourcePos,
        final int sourceSlot,
        final BlockPos targetPos,
        final int targetSlot,
        final Holder<LootItemFunction> modifier
    ) throws CommandSyntaxException {
        return setBlockItem(source, targetPos, targetSlot, applyModifier(source, modifier, getBlockItem(source, sourcePos, sourceSlot)));
    }

    private static int entityToBlock(
        final CommandSourceStack source, final Entity sourceEntity, final int sourceSlot, final BlockPos targetPos, final int targetSlot
    ) throws CommandSyntaxException {
        return setBlockItem(source, targetPos, targetSlot, getItemInSlot(sourceEntity, sourceSlot));
    }

    private static int entityToBlock(
        final CommandSourceStack source,
        final Entity sourceEntity,
        final int sourceSlot,
        final BlockPos targetPos,
        final int targetSlot,
        final Holder<LootItemFunction> modifier
    ) throws CommandSyntaxException {
        return setBlockItem(source, targetPos, targetSlot, applyModifier(source, modifier, getItemInSlot(sourceEntity, sourceSlot)));
    }

    private static int entityToEntities(
        final CommandSourceStack source,
        final Entity sourceEntity,
        final int sourceSlot,
        final Collection<? extends Entity> targetEntities,
        final int targetSlot
    ) throws CommandSyntaxException {
        return setEntityItem(source, targetEntities, targetSlot, getItemInSlot(sourceEntity, sourceSlot));
    }

    private static int entityToEntities(
        final CommandSourceStack source,
        final Entity sourceEntity,
        final int sourceSlot,
        final Collection<? extends Entity> targetEntities,
        final int targetSlot,
        final Holder<LootItemFunction> modifier
    ) throws CommandSyntaxException {
        return setEntityItem(source, targetEntities, targetSlot, applyModifier(source, modifier, getItemInSlot(sourceEntity, sourceSlot)));
    }

    private static ItemStack applyModifier(final CommandSourceStack source, final Holder<LootItemFunction> modifier, final ItemStack item) {
        ServerLevel level = source.getLevel();
        LootParams lootParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, source.getPosition())
            .withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity())
            .create(LootContextParamSets.COMMAND);
        LootContext context = new LootContext.Builder(lootParams).create(Optional.empty());
        context.pushVisitedElement(LootContext.createVisitedEntry(modifier.value()));
        ItemStack newItem = modifier.value().apply(item, context);
        newItem.limitSize(newItem.getMaxStackSize());
        return newItem;
    }

    private static ItemStack getItemInSlot(final SlotProvider slotProvider, final int slot) throws CommandSyntaxException {
        SlotAccess slotAccess = slotProvider.getSlot(slot);
        if (slotAccess == null) {
            throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slot);
        } else {
            return slotAccess.get().copy();
        }
    }

    private static ItemStack getBlockItem(final CommandSourceStack source, final BlockPos pos, final int slot) throws CommandSyntaxException {
        Container container = getContainer(source, pos, ERROR_SOURCE_NOT_A_CONTAINER);
        return getItemInSlot(container, slot);
    }
}