package net.minecraft.gametest.framework;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceSelectorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundGameTestHighlightPosPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.InCommandFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.mutable.MutableInt;

public class TestCommand {
    public static final int TEST_NEARBY_SEARCH_RADIUS = 15;
    public static final int TEST_FULL_SEARCH_RADIUS = 250;
    public static final int VERIFY_TEST_GRID_AXIS_SIZE = 10;
    public static final int VERIFY_TEST_BATCH_SIZE = 100;
    private static final int DEFAULT_CLEAR_RADIUS = 250;
    private static final int MAX_CLEAR_RADIUS = 1024;
    private static final int TEST_POS_Z_OFFSET_FROM_PLAYER = 3;
    private static final int DEFAULT_X_SIZE = 5;
    private static final int DEFAULT_Y_SIZE = 5;
    private static final int DEFAULT_Z_SIZE = 5;
    private static final SimpleCommandExceptionType CLEAR_NO_TESTS = new SimpleCommandExceptionType(
        Component.translatable("commands.test.clear.error.no_tests")
    );
    private static final SimpleCommandExceptionType RESET_NO_TESTS = new SimpleCommandExceptionType(
        Component.translatable("commands.test.reset.error.no_tests")
    );
    private static final SimpleCommandExceptionType TEST_INSTANCE_COULD_NOT_BE_FOUND = new SimpleCommandExceptionType(
        Component.translatable("commands.test.error.test_instance_not_found")
    );
    private static final SimpleCommandExceptionType NO_STRUCTURES_TO_EXPORT = new SimpleCommandExceptionType(
        Component.literal("Could not find any structures to export")
    );
    private static final SimpleCommandExceptionType NO_TEST_INSTANCES = new SimpleCommandExceptionType(
        Component.translatable("commands.test.error.no_test_instances")
    );
    private static final Dynamic3CommandExceptionType NO_TEST_CONTAINING = new Dynamic3CommandExceptionType(
        (x, y, z) -> Component.translatableEscape("commands.test.error.no_test_containing_pos", x, y, z)
    );
    private static final DynamicCommandExceptionType TOO_LARGE = new DynamicCommandExceptionType(
        size -> Component.translatableEscape("commands.test.error.too_large", size)
    );

    private static int reset(final TestFinder finder) throws CommandSyntaxException {
        stopTests();
        int count = toGameTestInfos(finder.source(), RetryOptions.noRetries(), finder).map(info -> resetGameTestInfo(finder.source(), info)).toList().size();
        if (count == 0) {
            throw CLEAR_NO_TESTS.create();
        }

        finder.source().sendSuccess(() -> Component.translatable("commands.test.reset.success", count), true);
        return count;
    }

    private static int clear(final TestFinder finder) throws CommandSyntaxException {
        stopTests();
        CommandSourceStack source = finder.source();
        ServerLevel level = source.getLevel();
        List<TestInstanceBlockEntity> tests = finder.findTestPos()
            .flatMap(pos -> level.getBlockEntity(pos, BlockEntityTypes.TEST_INSTANCE_BLOCK).stream())
            .toList();

        for (TestInstanceBlockEntity testInstanceBlockEntity : tests) {
            StructureUtils.clearSpaceForStructure(testInstanceBlockEntity.getTestBoundingBox(), level);
            testInstanceBlockEntity.removeBarriers();
            level.destroyBlock(testInstanceBlockEntity.getBlockPos(), false);
        }

        if (tests.isEmpty()) {
            throw CLEAR_NO_TESTS.create();
        }

        source.sendSuccess(() -> Component.translatable("commands.test.clear.success", tests.size()), true);
        return tests.size();
    }

    private static int export(final TestFinder finder) throws CommandSyntaxException {
        CommandSourceStack source = finder.source();
        ServerLevel level = source.getLevel();
        int count = 0;
        boolean allGood = true;

        for (Iterator<BlockPos> iterator = finder.findTestPos().iterator(); iterator.hasNext(); count++) {
            BlockPos pos = iterator.next();
            if (!(level.getBlockEntity(pos) instanceof TestInstanceBlockEntity blockEntity)) {
                throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
            }

            if (!blockEntity.exportTest(source::sendSystemMessage)) {
                allGood = false;
            }
        }

        if (count == 0) {
            throw NO_STRUCTURES_TO_EXPORT.create();
        }

        String message = "Exported " + count + " structures";
        finder.source().sendSuccess(() -> Component.literal(message), true);
        return allGood ? 0 : 1;
    }

    private static int verify(final TestFinder finder) {
        stopTests();
        CommandSourceStack source = finder.source();
        ServerLevel level = source.getLevel();
        BlockPos testPos = createTestPositionAround(source);
        Collection<GameTestInfo> infos = Stream.concat(
                toGameTestInfos(source, RetryOptions.noRetries(), finder), toGameTestInfo(source, RetryOptions.noRetries(), finder, 0)
            )
            .toList();
        FailedTestTracker.forgetFailedTests();
        Collection<GameTestBatch> batches = new ArrayList<>();

        for (GameTestInfo info : infos) {
            for (Rotation rotation : Rotation.values()) {
                Collection<GameTestInfo> transformedInfos = new ArrayList<>();

                for (int i = 0; i < 100; i++) {
                    GameTestInfo copyInfo = new GameTestInfo(info.getTestHolder(), rotation, level, new RetryOptions(1, true));
                    copyInfo.setTestBlockPos(info.getTestBlockPos());
                    transformedInfos.add(copyInfo);
                }

                GameTestBatch batch = GameTestBatchFactory.toGameTestBatch(transformedInfos, info.getTest().batch(), rotation.ordinal());
                batches.add(batch);
            }
        }

        StructureGridSpawner spawner = new StructureGridSpawner(testPos, 10, true);
        GameTestRunner runner = GameTestRunner.Builder.fromBatches(batches, level)
            .batcher(GameTestBatchFactory.fromGameTestInfo(100))
            .newStructureSpawner(spawner)
            .existingStructureSpawner(spawner)
            .haltOnError()
            .clearBetweenBatches()
            .build();
        return trackAndStartRunner(source, runner);
    }

    private static int run(final TestFinder finder, final RetryOptions retryOptions, final int extraRotationSteps, final int testsPerRow) {
        stopTests();
        CommandSourceStack source = finder.source();
        ServerLevel level = source.getLevel();
        BlockPos testPos = createTestPositionAround(source);
        Collection<GameTestInfo> infos = Stream.concat(
                toGameTestInfos(source, retryOptions, finder), toGameTestInfo(source, retryOptions, finder, extraRotationSteps)
            )
            .toList();
        if (infos.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.test.no_tests"), false);
            return 0;
        } else {
            FailedTestTracker.forgetFailedTests();
            source.sendSuccess(() -> Component.translatable("commands.test.run.running", infos.size()), false);
            GameTestRunner runner = GameTestRunner.Builder.fromInfo(infos, level)
                .newStructureSpawner(new StructureGridSpawner(testPos, testsPerRow, false))
                .build();
            return trackAndStartRunner(source, runner);
        }
    }

    private static int locate(final TestFinder finder) throws CommandSyntaxException {
        finder.source().sendSystemMessage(Component.translatable("commands.test.locate.started"));
        MutableInt structuresFound = new MutableInt(0);
        BlockPos sourcePos = BlockPos.containing(finder.source().getPosition());
        finder.findTestPos()
            .forEach(
                structurePos -> {
                    if (finder.source().getLevel().getBlockEntity(structurePos) instanceof TestInstanceBlockEntity testBlock) {
                        Direction var13 = testBlock.getRotation().rotate(Direction.NORTH);
                        BlockPos telportPosition = testBlock.getBlockPos().relative(var13, 2);
                        int teleportYRot = (int)var13.getOpposite().toYRot();
                        String tpCommand = String.format(
                            Locale.ROOT, "/tp @s %d %d %d %d 0", telportPosition.getX(), telportPosition.getY(), telportPosition.getZ(), teleportYRot
                        );
                        int dx = sourcePos.getX() - structurePos.getX();
                        int dz = sourcePos.getZ() - structurePos.getZ();
                        int distance = Mth.floor(Mth.sqrt(dx * dx + dz * dz));
                        MutableComponent coordinates = ComponentUtils.wrapInSquareBrackets(
                                Component.translatable("chat.coordinates", structurePos.getX(), structurePos.getY(), structurePos.getZ())
                            )
                            .withStyle(
                                s -> s.withColor(ChatFormatting.GREEN)
                                    .withClickEvent(new ClickEvent.SuggestCommand(tpCommand))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                            );
                        finder.source().sendSuccess(() -> Component.translatable("commands.test.locate.found", coordinates, distance), false);
                        structuresFound.increment();
                    }
                }
            );
        int structures = structuresFound.intValue();
        if (structures == 0) {
            throw NO_TEST_INSTANCES.create();
        }

        finder.source().sendSuccess(() -> Component.translatable("commands.test.locate.done", structures), true);
        return structures;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
        final ArgumentBuilder<CommandSourceStack, ?> runArgument,
        final InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> finder,
        final Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> then
    ) {
        return runArgument.executes(c -> run(finder.apply(c), RetryOptions.noRetries(), 0, 8))
            .then(
                Commands.argument("numberOfTimes", IntegerArgumentType.integer(0))
                    .executes(c -> run(finder.apply(c), new RetryOptions(IntegerArgumentType.getInteger(c, "numberOfTimes"), false), 0, 8))
                    .then(
                        then.apply(
                            Commands.argument("untilFailed", BoolArgumentType.bool())
                                .executes(
                                    c -> run(
                                        finder.apply(c),
                                        new RetryOptions(IntegerArgumentType.getInteger(c, "numberOfTimes"), BoolArgumentType.getBool(c, "untilFailed")),
                                        0,
                                        8
                                    )
                                )
                        )
                    )
            );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
        final ArgumentBuilder<CommandSourceStack, ?> runArgument, final InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> finder
    ) {
        return runWithRetryOptions(runArgument, finder, a -> a);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptionsAndBuildInfo(
        final ArgumentBuilder<CommandSourceStack, ?> runArgument, final InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> finder
    ) {
        return runWithRetryOptions(
            runArgument,
            finder,
            then -> then.then(
                Commands.argument("rotationSteps", IntegerArgumentType.integer())
                    .executes(
                        c -> run(
                            finder.apply(c),
                            new RetryOptions(IntegerArgumentType.getInteger(c, "numberOfTimes"), BoolArgumentType.getBool(c, "untilFailed")),
                            IntegerArgumentType.getInteger(c, "rotationSteps"),
                            8
                        )
                    )
                    .then(
                        Commands.argument("testsPerRow", IntegerArgumentType.integer())
                            .executes(
                                c -> run(
                                    finder.apply(c),
                                    new RetryOptions(IntegerArgumentType.getInteger(c, "numberOfTimes"), BoolArgumentType.getBool(c, "untilFailed")),
                                    IntegerArgumentType.getInteger(c, "rotationSteps"),
                                    IntegerArgumentType.getInteger(c, "testsPerRow")
                                )
                            )
                    )
            )
        );
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        ArgumentBuilder<CommandSourceStack, ?> runFailedWithRequiredTestsFlag = runWithRetryOptionsAndBuildInfo(
            Commands.argument("onlyRequiredTests", BoolArgumentType.bool()),
            c -> TestFinder.builder().failedTests(c, BoolArgumentType.getBool(c, "onlyRequiredTests"))
        );
        LiteralArgumentBuilder<CommandSourceStack> testCommand = Commands.literal("test")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(
                Commands.literal("run")
                    .then(
                        runWithRetryOptionsAndBuildInfo(
                            Commands.argument("tests", ResourceSelectorArgument.resourceSelector(context, Registries.TEST_INSTANCE)),
                            c -> TestFinder.builder().byResourceSelection(c, ResourceSelectorArgument.getSelectedResources(c, "tests"))
                        )
                    )
            )
            .then(
                Commands.literal("runmultiple")
                    .then(
                        Commands.argument("tests", ResourceSelectorArgument.resourceSelector(context, Registries.TEST_INSTANCE))
                            .executes(
                                c -> run(
                                    TestFinder.builder().byResourceSelection(c, ResourceSelectorArgument.getSelectedResources(c, "tests")),
                                    RetryOptions.noRetries(),
                                    0,
                                    8
                                )
                            )
                            .then(
                                Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(
                                        c -> run(
                                            TestFinder.builder()
                                                .createMultipleCopies(IntegerArgumentType.getInteger(c, "amount"))
                                                .byResourceSelection(c, ResourceSelectorArgument.getSelectedResources(c, "tests")),
                                            RetryOptions.noRetries(),
                                            0,
                                            8
                                        )
                                    )
                            )
                    )
            )
            .then(runWithRetryOptions(Commands.literal("runthese"), TestFinder.builder()::allNearby))
            .then(runWithRetryOptions(Commands.literal("runclosest"), TestFinder.builder()::nearest))
            .then(runWithRetryOptions(Commands.literal("runthat"), TestFinder.builder()::lookedAt))
            .then(runWithRetryOptionsAndBuildInfo(Commands.literal("runfailed").then(runFailedWithRequiredTestsFlag), TestFinder.builder()::failedTests))
            .then(
                Commands.literal("verify")
                    .then(
                        Commands.argument("tests", ResourceSelectorArgument.resourceSelector(context, Registries.TEST_INSTANCE))
                            .executes(c -> verify(TestFinder.builder().byResourceSelection(c, ResourceSelectorArgument.getSelectedResources(c, "tests"))))
                    )
            )
            .then(
                Commands.literal("locate")
                    .then(
                        Commands.argument("tests", ResourceSelectorArgument.resourceSelector(context, Registries.TEST_INSTANCE))
                            .executes(c -> locate(TestFinder.builder().byResourceSelection(c, ResourceSelectorArgument.getSelectedResources(c, "tests"))))
                    )
            )
            .then(Commands.literal("resetclosest").executes(c -> reset(TestFinder.builder().nearest(c))))
            .then(Commands.literal("resetthese").executes(c -> reset(TestFinder.builder().allNearby(c))))
            .then(Commands.literal("resetthat").executes(c -> reset(TestFinder.builder().lookedAt(c))))
            .then(Commands.literal("clearthat").executes(c -> clear(TestFinder.builder().lookedAt(c))))
            .then(Commands.literal("clearthese").executes(c -> clear(TestFinder.builder().allNearby(c))))
            .then(
                Commands.literal("clearall")
                    .executes(c -> clear(TestFinder.builder().radius(c, 250)))
                    .then(
                        Commands.argument("radius", IntegerArgumentType.integer())
                            .executes(c -> clear(TestFinder.builder().radius(c, Mth.clamp(IntegerArgumentType.getInteger(c, "radius"), 0, 1024))))
                    )
            )
            .then(Commands.literal("stop").executes(c -> stopTests()))
            .then(
                Commands.literal("pos")
                    .executes(c -> showPos(c.getSource(), "pos"))
                    .then(Commands.argument("var", StringArgumentType.word()).executes(c -> showPos(c.getSource(), StringArgumentType.getString(c, "var"))))
            )
            .then(
                Commands.literal("create")
                    .then(
                        Commands.argument("id", IdentifierArgument.id())
                            .suggests(TestCommand::suggestTestFunction)
                            .executes(c -> createNewStructure(c.getSource(), IdentifierArgument.getId(c, "id"), 5, 5, 5))
                            .then(
                                Commands.argument("width", IntegerArgumentType.integer())
                                    .executes(
                                        c -> createNewStructure(
                                            c.getSource(),
                                            IdentifierArgument.getId(c, "id"),
                                            IntegerArgumentType.getInteger(c, "width"),
                                            IntegerArgumentType.getInteger(c, "width"),
                                            IntegerArgumentType.getInteger(c, "width")
                                        )
                                    )
                                    .then(
                                        Commands.argument("height", IntegerArgumentType.integer())
                                            .then(
                                                Commands.argument("depth", IntegerArgumentType.integer())
                                                    .executes(
                                                        c -> createNewStructure(
                                                            c.getSource(),
                                                            IdentifierArgument.getId(c, "id"),
                                                            IntegerArgumentType.getInteger(c, "width"),
                                                            IntegerArgumentType.getInteger(c, "height"),
                                                            IntegerArgumentType.getInteger(c, "depth")
                                                        )
                                                    )
                                            )
                                    )
                            )
                    )
            );
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            testCommand = testCommand.then(
                    Commands.literal("export")
                        .then(
                            Commands.argument("test", ResourceArgument.resource(context, Registries.TEST_INSTANCE))
                                .executes(c -> exportTestStructure(c.getSource(), ResourceArgument.getResource(c, "test", Registries.TEST_INSTANCE)))
                        )
                )
                .then(Commands.literal("exportclosest").executes(c -> export(TestFinder.builder().nearest(c))))
                .then(Commands.literal("exportthese").executes(c -> export(TestFinder.builder().allNearby(c))))
                .then(Commands.literal("exportthat").executes(c -> export(TestFinder.builder().lookedAt(c))));
        }

        dispatcher.register(testCommand);
    }

    public static CompletableFuture<Suggestions> suggestTestFunction(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        Stream<String> testNamesStream = context.getSource()
            .registryAccess()
            .lookupOrThrow(Registries.TEST_FUNCTION)
            .listElements()
            .map(Holder::getRegisteredName);
        return SharedSuggestionProvider.suggest(testNamesStream, builder);
    }

    private static int resetGameTestInfo(final CommandSourceStack source, final GameTestInfo testInfo) {
        TestInstanceBlockEntity blockEntity = testInfo.getTestInstanceBlockEntity();
        blockEntity.resetTest(source::sendSystemMessage);
        return 1;
    }

    private static Stream<GameTestInfo> toGameTestInfos(final CommandSourceStack source, final RetryOptions retryOptions, final TestPosFinder finder) {
        return finder.findTestPos().map(pos -> createGameTestInfo(pos, source, retryOptions)).flatMap(Optional::stream);
    }

    private static Stream<GameTestInfo> toGameTestInfo(
        final CommandSourceStack source, final RetryOptions retryOptions, final TestInstanceFinder finder, final int rotationSteps
    ) {
        return finder.findTests()
            .filter(test -> verifyStructureExists(source, test.value().structure()))
            .map(
                test -> new GameTestInfo(
                    (Holder.Reference<GameTestInstance>)test, StructureUtils.getRotationForRotationSteps(rotationSteps), source.getLevel(), retryOptions
                )
            );
    }

    private static Optional<GameTestInfo> createGameTestInfo(final BlockPos testBlockPos, final CommandSourceStack source, final RetryOptions retryOptions) {
        ServerLevel level = source.getLevel();
        if (level.getBlockEntity(testBlockPos) instanceof TestInstanceBlockEntity blockEntity) {
            Optional<Holder.Reference<GameTestInstance>> maybeTest = blockEntity.test()
                .flatMap(source.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE)::get);
            if (maybeTest.isEmpty()) {
                source.sendFailure(Component.translatable("commands.test.error.non_existant_test", blockEntity.getTestName()));
                return Optional.empty();
            } else {
                Holder.Reference<GameTestInstance> test = maybeTest.get();
                GameTestInfo testInfo = new GameTestInfo(test, blockEntity.getRotation(), level, retryOptions);
                testInfo.setTestBlockPos(testBlockPos);
                return !verifyStructureExists(source, testInfo.getStructure()) ? Optional.empty() : Optional.of(testInfo);
            }
        } else {
            source.sendFailure(
                Component.translatable("commands.test.error.test_instance_not_found.position", testBlockPos.getX(), testBlockPos.getY(), testBlockPos.getZ())
            );
            return Optional.empty();
        }
    }

    private static int createNewStructure(final CommandSourceStack source, final Identifier id, final int xSize, final int ySize, final int zSize) throws CommandSyntaxException {
        if (xSize <= 48 && ySize <= 48 && zSize <= 48) {
            ServerLevel level = source.getLevel();
            BlockPos testPos = createTestPositionAround(source);
            TestInstanceBlockEntity test = StructureUtils.createNewEmptyTest(id, testPos, new Vec3i(xSize, ySize, zSize), Rotation.NONE, level);
            BlockPos low = test.getStructurePos();
            BlockPos high = low.offset(xSize - 1, 0, zSize - 1);
            BlockPos.betweenClosedStream(low, high).forEach(blockPos -> level.setBlockAndUpdate(blockPos, Blocks.BEDROCK.defaultBlockState()));
            source.sendSuccess(() -> Component.translatable("commands.test.create.success", test.getTestName()), true);
            return 1;
        } else {
            throw TOO_LARGE.create(48);
        }
    }

    private static int showPos(final CommandSourceStack source, final String varName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockHitResult pick = (BlockHitResult)player.pick(10.0, 1.0F, false);
        BlockPos targetPosAbsolute = pick.getBlockPos();
        ServerLevel level = source.getLevel();
        Optional<BlockPos> testBlockPos = StructureUtils.findTestContainingPos(targetPosAbsolute, 15, level);
        if (testBlockPos.isEmpty()) {
            testBlockPos = StructureUtils.findTestContainingPos(targetPosAbsolute, 250, level);
        }

        if (testBlockPos.isEmpty()) {
            throw NO_TEST_CONTAINING.create(targetPosAbsolute.getX(), targetPosAbsolute.getY(), targetPosAbsolute.getZ());
        } else if (level.getBlockEntity(testBlockPos.get()) instanceof TestInstanceBlockEntity testBlockEntity) {
            BlockPos var13 = testBlockEntity.getStructurePos();
            BlockPos targetPosRelative = targetPosAbsolute.subtract(var13);
            String targetPosDescription = targetPosRelative.getX() + ", " + targetPosRelative.getY() + ", " + targetPosRelative.getZ();
            String testName = testBlockEntity.getTestName().getString();
            MutableComponent coords = Component.translatable(
                    "commands.test.coordinates", targetPosRelative.getX(), targetPosRelative.getY(), targetPosRelative.getZ()
                )
                .setStyle(
                    Style.EMPTY
                        .withBold(true)
                        .withColor(ChatFormatting.GREEN)
                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("commands.test.coordinates.copy")))
                        .withClickEvent(new ClickEvent.CopyToClipboard("final BlockPos " + varName + " = new BlockPos(" + targetPosDescription + ");"))
                );
            source.sendSuccess(() -> Component.translatable("commands.test.relative_position", testName, coords), false);
            player.connection.send(new ClientboundGameTestHighlightPosPacket(targetPosAbsolute, targetPosRelative));
            return 1;
        } else {
            throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
        }
    }

    private static int stopTests() {
        GameTestTicker.SINGLETON.clear();
        return 1;
    }

    public static int trackAndStartRunner(final CommandSourceStack source, final GameTestRunner runner) {
        runner.addListener(new TestCommand.TestBatchSummaryDisplayer(source));
        MultipleTestTracker tracker = new MultipleTestTracker(runner.getTestInfos());
        tracker.addListener(new TestCommand.TestSummaryDisplayer(source, tracker));
        tracker.addFailureListener(testInfo -> FailedTestTracker.rememberFailedTest(testInfo.getTestHolder()));
        runner.start();
        return 1;
    }

    private static int exportTestStructure(final CommandSourceStack source, final Holder<GameTestInstance> test) {
        return !TestInstanceBlockEntity.export(source.getLevel(), test.value().structure(), source::sendSystemMessage) ? 0 : 1;
    }

    private static boolean verifyStructureExists(final CommandSourceStack source, final Identifier structure) {
        if (source.getLevel().getStructureManager().get(structure).isEmpty()) {
            source.sendFailure(Component.translatable("commands.test.error.structure_not_found", Component.translationArg(structure)));
            return false;
        } else {
            return true;
        }
    }

    private static BlockPos createTestPositionAround(final CommandSourceStack source) {
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        int surfaceY = source.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, playerPos).getY();
        return new BlockPos(playerPos.getX(), surfaceY, playerPos.getZ() + 3);
    }

    private record TestBatchSummaryDisplayer(CommandSourceStack source) implements GameTestBatchListener {
        @Override
        public void testBatchStarting(final GameTestBatch batch) {
            this.source.sendSuccess(() -> Component.translatable("commands.test.batch.starting", batch.environment().getRegisteredName(), batch.index()), true);
        }

        @Override
        public void testBatchFinished(final GameTestBatch batch) {
        }
    }

    public record TestSummaryDisplayer(CommandSourceStack source, MultipleTestTracker tracker) implements GameTestListener {
        @Override
        public void testStructureLoaded(final GameTestInfo testInfo) {
        }

        @Override
        public void testPassed(final GameTestInfo testInfo, final GameTestRunner runner) {
            this.showTestSummaryIfAllDone();
        }

        @Override
        public void testFailed(final GameTestInfo testInfo, final GameTestRunner runner) {
            this.showTestSummaryIfAllDone();
        }

        @Override
        public void testAddedForRerun(final GameTestInfo original, final GameTestInfo copy, final GameTestRunner runner) {
            this.tracker.addTestToTrack(copy);
        }

        private void showTestSummaryIfAllDone() {
            if (this.tracker.isDone()) {
                this.source
                    .sendSuccess(() -> Component.translatable("commands.test.summary", this.tracker.getTotalCount()).withStyle(ChatFormatting.WHITE), true);
                if (this.tracker.hasFailedRequired()) {
                    this.source.sendFailure(Component.translatable("commands.test.summary.failed", this.tracker.getFailedRequiredCount()));
                } else {
                    this.source.sendSuccess(() -> Component.translatable("commands.test.summary.all_required_passed").withStyle(ChatFormatting.GREEN), true);
                }

                if (this.tracker.hasFailedOptional()) {
                    this.source.sendSystemMessage(Component.translatable("commands.test.summary.optional_failed", this.tracker.getFailedOptionalCount()));
                }
            }
        }
    }
}