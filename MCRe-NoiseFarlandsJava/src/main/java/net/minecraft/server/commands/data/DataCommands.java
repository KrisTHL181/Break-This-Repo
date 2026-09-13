package net.minecraft.server.commands.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.PrimitiveTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class DataCommands {
    private static final SimpleCommandExceptionType ERROR_MERGE_UNCHANGED = new SimpleCommandExceptionType(Component.translatable("commands.data.merge.failed"));
    private static final DynamicCommandExceptionType ERROR_GET_NOT_NUMBER = new DynamicCommandExceptionType(
        path -> Component.translatableEscape("commands.data.get.invalid", path)
    );
    private static final DynamicCommandExceptionType ERROR_GET_NON_EXISTENT = new DynamicCommandExceptionType(
        path -> Component.translatableEscape("commands.data.get.unknown", path)
    );
    private static final SimpleCommandExceptionType ERROR_MULTIPLE_TAGS = new SimpleCommandExceptionType(Component.translatable("commands.data.get.multiple"));
    private static final DynamicCommandExceptionType ERROR_EXPECTED_OBJECT = new DynamicCommandExceptionType(
        node -> Component.translatableEscape("commands.data.modify.expected_object", node)
    );
    private static final DynamicCommandExceptionType ERROR_EXPECTED_VALUE = new DynamicCommandExceptionType(
        node -> Component.translatableEscape("commands.data.modify.expected_value", node)
    );
    private static final Dynamic2CommandExceptionType ERROR_INVALID_SUBSTRING = new Dynamic2CommandExceptionType(
        (start, end) -> Component.translatableEscape("commands.data.modify.invalid_substring", start, end)
    );
    public static final List<Function<String, DataCommands.DataProvider>> ALL_PROVIDERS = ImmutableList.of(
        EntityDataAccessor.PROVIDER, BlockDataAccessor.PROVIDER, StorageDataAccessor.PROVIDER
    );
    public static final List<DataCommands.DataProvider> TARGET_PROVIDERS = ALL_PROVIDERS.stream()
        .map(f -> f.apply("target"))
        .collect(ImmutableList.toImmutableList());
    public static final List<DataCommands.DataProvider> SOURCE_PROVIDERS = ALL_PROVIDERS.stream()
        .map(f -> f.apply("source"))
        .collect(ImmutableList.toImmutableList());

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("data").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        for (DataCommands.DataProvider targetProvider : TARGET_PROVIDERS) {
            root.then(
                    targetProvider.wrap(
                        Commands.literal("merge"),
                        p -> p.then(
                            Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                .executes(c -> mergeData(c.getSource(), targetProvider.access(c), CompoundTagArgument.getCompoundTag(c, "nbt")))
                        )
                    )
                )
                .then(
                    targetProvider.wrap(
                        Commands.literal("get"),
                        p -> p.executes(c -> getData(c.getSource(), targetProvider.access(c)))
                            .then(
                                Commands.argument("path", NbtPathArgument.nbtPath())
                                    .executes(c -> getData(c.getSource(), targetProvider.access(c), NbtPathArgument.getPath(c, "path")))
                                    .then(
                                        Commands.argument("scale", DoubleArgumentType.doubleArg())
                                            .executes(
                                                c -> getNumeric(
                                                    c.getSource(),
                                                    targetProvider.access(c),
                                                    NbtPathArgument.getPath(c, "path"),
                                                    DoubleArgumentType.getDouble(c, "scale")
                                                )
                                            )
                                    )
                            )
                    )
                )
                .then(
                    targetProvider.wrap(
                        Commands.literal("remove"),
                        p -> p.then(
                            Commands.argument("path", NbtPathArgument.nbtPath())
                                .executes(c -> removeData(c.getSource(), targetProvider.access(c), NbtPathArgument.getPath(c, "path")))
                        )
                    )
                )
                .then(
                    decorateModification(
                        (parent, rest) -> parent.then(
                                Commands.literal("insert")
                                    .then(
                                        Commands.argument("index", IntegerArgumentType.integer())
                                            .then(
                                                rest.create(
                                                    (context, target, targetPath, source) -> targetPath.insert(
                                                        IntegerArgumentType.getInteger(context, "index"), target, source
                                                    )
                                                )
                                            )
                                    )
                            )
                            .then(Commands.literal("prepend").then(rest.create((context, target, targetPath, source) -> targetPath.insert(0, target, source))))
                            .then(Commands.literal("append").then(rest.create((context, target, targetPath, source) -> targetPath.insert(-1, target, source))))
                            .then(
                                Commands.literal("set")
                                    .then(rest.create((context, target, targetPath, source) -> targetPath.set(target, Iterables.getLast(source))))
                            )
                            .then(Commands.literal("merge").then(rest.create((context, target, targetPath, source) -> {
                                CompoundTag combinedSources = new CompoundTag();

                                for (Tag sourceTag : source) {
                                    if (NbtPathArgument.NbtPath.isTooDeep(sourceTag, 0)) {
                                        throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
                                    }

                                    if (!(sourceTag instanceof CompoundTag tag)) {
                                        throw ERROR_EXPECTED_OBJECT.create(sourceTag);
                                    }

                                    combinedSources.merge(tag);
                                }

                                Collection<Tag> targets = targetPath.getOrCreate(target, CompoundTag::new);
                                int changedCount = 0;

                                for (Tag targetTag : targets) {
                                    if (!(targetTag instanceof CompoundTag targetObject)) {
                                        throw ERROR_EXPECTED_OBJECT.create(targetTag);
                                    }

                                    CompoundTag originalTarget = targetObject.copy();
                                    targetObject.merge(combinedSources);
                                    changedCount += originalTarget.equals(targetObject) ? 0 : 1;
                                }

                                return changedCount;
                            })))
                    )
                );
        }

        dispatcher.register(root);
    }

    private static String getAsText(final Tag tag) throws CommandSyntaxException {
        return switch (tag) {
            case StringTag(String value) -> value;
            case PrimitiveTag primitiveTag -> primitiveTag.toString();
            default -> throw ERROR_EXPECTED_VALUE.create(tag);
        };
    }

    private static List<Tag> stringifyTagList(final List<Tag> source, final DataCommands.StringProcessor stringProcessor) throws CommandSyntaxException {
        List<Tag> result = new ArrayList<>(source.size());

        for (Tag tag : source) {
            String text = getAsText(tag);
            result.add(StringTag.valueOf(stringProcessor.process(text)));
        }

        return result;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> decorateModification(
        final BiConsumer<ArgumentBuilder<CommandSourceStack, ?>, DataCommands.DataManipulatorDecorator> nodeSupplier
    ) {
        LiteralArgumentBuilder<CommandSourceStack> modify = Commands.literal("modify");

        for (DataCommands.DataProvider targetProvider : TARGET_PROVIDERS) {
            targetProvider.wrap(
                modify,
                t -> {
                    ArgumentBuilder<CommandSourceStack, ?> targetPathNode = Commands.argument("targetPath", NbtPathArgument.nbtPath());

                    for (DataCommands.DataProvider sourceProvider : SOURCE_PROVIDERS) {
                        nodeSupplier.accept(
                            targetPathNode,
                            manipulator -> sourceProvider.wrap(
                                Commands.literal("from"),
                                s -> s.executes(c -> manipulateData(c, targetProvider, manipulator, getSingletonSource(c, sourceProvider)))
                                    .then(
                                        Commands.argument("sourcePath", NbtPathArgument.nbtPath())
                                            .executes(c -> manipulateData(c, targetProvider, manipulator, resolveSourcePath(c, sourceProvider)))
                                    )
                            )
                        );
                        nodeSupplier.accept(
                            targetPathNode,
                            manipulator -> sourceProvider.wrap(
                                Commands.literal("string"),
                                s -> s.executes(
                                        c -> manipulateData(c, targetProvider, manipulator, stringifyTagList(getSingletonSource(c, sourceProvider), str -> str))
                                    )
                                    .then(
                                        Commands.argument("sourcePath", NbtPathArgument.nbtPath())
                                            .executes(
                                                c -> manipulateData(
                                                    c, targetProvider, manipulator, stringifyTagList(resolveSourcePath(c, sourceProvider), str -> str)
                                                )
                                            )
                                            .then(
                                                Commands.argument("start", IntegerArgumentType.integer())
                                                    .executes(
                                                        c -> manipulateData(
                                                            c,
                                                            targetProvider,
                                                            manipulator,
                                                            stringifyTagList(
                                                                resolveSourcePath(c, sourceProvider),
                                                                str -> substring(str, IntegerArgumentType.getInteger(c, "start"))
                                                            )
                                                        )
                                                    )
                                                    .then(
                                                        Commands.argument("end", IntegerArgumentType.integer())
                                                            .executes(
                                                                c -> manipulateData(
                                                                    c,
                                                                    targetProvider,
                                                                    manipulator,
                                                                    stringifyTagList(
                                                                        resolveSourcePath(c, sourceProvider),
                                                                        str -> substring(
                                                                            str,
                                                                            IntegerArgumentType.getInteger(c, "start"),
                                                                            IntegerArgumentType.getInteger(c, "end")
                                                                        )
                                                                    )
                                                                )
                                                            )
                                                    )
                                            )
                                    )
                            )
                        );
                    }

                    nodeSupplier.accept(
                        targetPathNode, manipulator -> Commands.literal("value").then(Commands.argument("value", NbtTagArgument.nbtTag()).executes(c -> {
                            List<Tag> source = Collections.singletonList(NbtTagArgument.getNbtTag(c, "value"));
                            return manipulateData(c, targetProvider, manipulator, source);
                        }))
                    );
                    return t.then(targetPathNode);
                }
            );
        }

        return modify;
    }

    private static String validatedSubstring(final String input, final int start, final int end) throws CommandSyntaxException {
        if (start >= 0 && end <= input.length() && start <= end) {
            return input.substring(start, end);
        } else {
            throw ERROR_INVALID_SUBSTRING.create(start, end);
        }
    }

    private static String substring(final String input, final int start, final int end) throws CommandSyntaxException {
        int length = input.length();
        int absoluteStart = getOffset(start, length);
        int absoluteEnd = getOffset(end, length);
        return validatedSubstring(input, absoluteStart, absoluteEnd);
    }

    private static String substring(final String input, final int start) throws CommandSyntaxException {
        int length = input.length();
        return validatedSubstring(input, getOffset(start, length), length);
    }

    private static int getOffset(final int index, final int length) {
        return index >= 0 ? index : length + index;
    }

    private static List<Tag> getSingletonSource(final CommandContext<CommandSourceStack> context, final DataCommands.DataProvider sourceProvider) throws CommandSyntaxException {
        DataAccessor source = sourceProvider.access(context);
        return Collections.singletonList(source.getData());
    }

    private static List<Tag> resolveSourcePath(final CommandContext<CommandSourceStack> context, final DataCommands.DataProvider sourceProvider) throws CommandSyntaxException {
        DataAccessor source = sourceProvider.access(context);
        NbtPathArgument.NbtPath sourcePath = NbtPathArgument.getPath(context, "sourcePath");
        return sourcePath.get(source.getData());
    }

    private static int manipulateData(
        final CommandContext<CommandSourceStack> context,
        final DataCommands.DataProvider targetProvider,
        final DataCommands.DataManipulator manipulator,
        final List<Tag> source
    ) throws CommandSyntaxException {
        DataAccessor target = targetProvider.access(context);
        NbtPathArgument.NbtPath targetPath = NbtPathArgument.getPath(context, "targetPath");
        CompoundTag targetData = target.getData();
        int result = manipulator.modify(context, targetData, targetPath, source);
        if (result == 0) {
            throw ERROR_MERGE_UNCHANGED.create();
        }

        target.setData(targetData);
        context.getSource().sendSuccess(() -> target.getModifiedSuccess(), true);
        return result;
    }

    private static int removeData(final CommandSourceStack source, final DataAccessor accessor, final NbtPathArgument.NbtPath path) throws CommandSyntaxException {
        CompoundTag result = accessor.getData();
        int count = path.remove(result);
        if (count == 0) {
            throw ERROR_MERGE_UNCHANGED.create();
        }

        accessor.setData(result);
        source.sendSuccess(() -> accessor.getModifiedSuccess(), true);
        return count;
    }

    public static Tag getSingleTag(final NbtPathArgument.NbtPath path, final DataAccessor accessor) throws CommandSyntaxException {
        Collection<Tag> tags = path.get(accessor.getData());
        Iterator<Tag> iterator = tags.iterator();
        Tag result = iterator.next();
        if (iterator.hasNext()) {
            throw ERROR_MULTIPLE_TAGS.create();
        } else {
            return result;
        }
    }

    private static int getData(final CommandSourceStack source, final DataAccessor accessor, final NbtPathArgument.NbtPath path) throws CommandSyntaxException {
        Tag tag = getSingleTag(path, accessor);

        int result = switch (tag) {
            case NumericTag numericTag -> Mth.floor(numericTag.doubleValue());
            case CollectionTag collectionTag -> collectionTag.size();
            case CompoundTag compoundTag -> compoundTag.size();
            case StringTag(String value) -> value.length();
            case EndTag ignored -> throw ERROR_GET_NON_EXISTENT.create(path.toString());
            default -> throw new MatchException(null, null);
        };
        source.sendSuccess(() -> accessor.getPrintSuccess(tag), false);
        return result;
    }

    private static int getNumeric(final CommandSourceStack source, final DataAccessor accessor, final NbtPathArgument.NbtPath path, final double scale) throws CommandSyntaxException {
        if (getSingleTag(path, accessor) instanceof NumericTag numericTag) {
            int result = Mth.floor(numericTag.doubleValue() * scale);
            source.sendSuccess(() -> accessor.getPrintSuccess(path, scale, result), false);
            return result;
        } else {
            throw ERROR_GET_NOT_NUMBER.create(path.toString());
        }
    }

    private static int getData(final CommandSourceStack source, final DataAccessor accessor) throws CommandSyntaxException {
        CompoundTag data = accessor.getData();
        source.sendSuccess(() -> accessor.getPrintSuccess(data), false);
        return 1;
    }

    private static int mergeData(final CommandSourceStack source, final DataAccessor accessor, final CompoundTag nbt) throws CommandSyntaxException {
        CompoundTag old = accessor.getData();
        if (NbtPathArgument.NbtPath.isTooDeep(nbt, 0)) {
            throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
        }

        CompoundTag result = old.copy().merge(nbt);
        if (old.equals(result)) {
            throw ERROR_MERGE_UNCHANGED.create();
        }

        accessor.setData(result);
        source.sendSuccess(() -> accessor.getModifiedSuccess(), true);
        return 1;
    }

    @FunctionalInterface
    private interface DataManipulator {
        int modify(CommandContext<CommandSourceStack> context, CompoundTag targetData, NbtPathArgument.NbtPath targetPath, List<Tag> source) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private interface DataManipulatorDecorator {
        ArgumentBuilder<CommandSourceStack, ?> create(DataCommands.DataManipulator manipulator);
    }

    public interface DataProvider {
        DataAccessor access(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

        ArgumentBuilder<CommandSourceStack, ?> wrap(
            ArgumentBuilder<CommandSourceStack, ?> parent, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> function
        );
    }

    @FunctionalInterface
    private interface StringProcessor {
        String process(String string) throws CommandSyntaxException;
    }
}