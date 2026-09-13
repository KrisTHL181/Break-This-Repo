package net.minecraft.gametest.framework;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
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
   private static final SimpleCommandExceptionType CLEAR_NO_TESTS = new SimpleCommandExceptionType(Component.translatable("commands.test.clear.error.no_tests"));
   private static final SimpleCommandExceptionType RESET_NO_TESTS = new SimpleCommandExceptionType(Component.translatable("commands.test.reset.error.no_tests"));
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
      (p_389834_, p_389835_, p_389836_) -> Component.translatableEscape("commands.test.error.no_test_containing_pos", p_389834_, p_389835_, p_389836_)
   );
   private static final DynamicCommandExceptionType TOO_LARGE = new DynamicCommandExceptionType(
      p_389800_ -> Component.translatableEscape("commands.test.error.too_large", p_389800_)
   );

   private static int reset(TestFinder p_397105_) throws CommandSyntaxException {
      stopTests();
      int i = toGameTestInfos(p_397105_.source(), RetryOptions.noRetries(), p_397105_)
         .map(p_389815_ -> resetGameTestInfo(p_397105_.source(), p_389815_))
         .toList()
         .size();
      if (i == 0) {
         throw CLEAR_NO_TESTS.create();
      }

      p_397105_.source().sendSuccess(() -> Component.translatable("commands.test.reset.success", i), true);
      return i;
   }

   private static int clear(TestFinder p_395438_) throws CommandSyntaxException {
      stopTests();
      CommandSourceStack commandsourcestack = p_395438_.source();
      ServerLevel serverlevel = commandsourcestack.getLevel();
      List<TestInstanceBlockEntity> list = p_395438_.findTestPos()
         .flatMap(p_389831_ -> serverlevel.getBlockEntity(p_389831_, BlockEntityType.TEST_INSTANCE_BLOCK).stream())
         .toList();

      for (TestInstanceBlockEntity testinstanceblockentity : list) {
         StructureUtils.clearSpaceForStructure(testinstanceblockentity.getStructureBoundingBox(), serverlevel);
         testinstanceblockentity.removeBarriers();
         serverlevel.destroyBlock(testinstanceblockentity.getBlockPos(), false);
      }

      if (list.isEmpty()) {
         throw CLEAR_NO_TESTS.create();
      }

      commandsourcestack.sendSuccess(() -> Component.translatable("commands.test.clear.success", list.size()), true);
      return list.size();
   }

   private static int export(TestFinder p_393674_) throws CommandSyntaxException {
      CommandSourceStack commandsourcestack = p_393674_.source();
      ServerLevel serverlevel = commandsourcestack.getLevel();
      int i = 0;
      boolean flag = true;

      for (Iterator<BlockPos> iterator = p_393674_.findTestPos().iterator(); iterator.hasNext(); i++) {
         BlockPos blockpos = iterator.next();
         if (!(serverlevel.getBlockEntity(blockpos) instanceof TestInstanceBlockEntity testinstanceblockentity)) {
            throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
         }

         if (!testinstanceblockentity.exportTest(commandsourcestack::sendSystemMessage)) {
            flag = false;
         }
      }

      if (i == 0) {
         throw NO_STRUCTURES_TO_EXPORT.create();
      }

      String s = "Exported " + i + " structures";
      p_393674_.source().sendSuccess(() -> Component.literal(s), true);
      return flag ? 0 : 1;
   }

   private static int verify(TestFinder p_394697_) {
      stopTests();
      CommandSourceStack commandsourcestack = p_394697_.source();
      ServerLevel serverlevel = commandsourcestack.getLevel();
      BlockPos blockpos = createTestPositionAround(commandsourcestack);
      Collection<GameTestInfo> collection = Stream.concat(
            toGameTestInfos(commandsourcestack, RetryOptions.noRetries(), p_394697_),
            toGameTestInfo(commandsourcestack, RetryOptions.noRetries(), p_394697_, 0)
         )
         .toList();
      FailedTestTracker.forgetFailedTests();
      Collection<GameTestBatch> collection1 = new ArrayList<>();

      for (GameTestInfo gametestinfo : collection) {
         for (Rotation rotation : Rotation.values()) {
            Collection<GameTestInfo> collection2 = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
               GameTestInfo gametestinfo1 = new GameTestInfo(gametestinfo.getTestHolder(), rotation, serverlevel, new RetryOptions(1, true));
               gametestinfo1.setTestBlockPos(gametestinfo.getTestBlockPos());
               collection2.add(gametestinfo1);
            }

            GameTestBatch gametestbatch = GameTestBatchFactory.toGameTestBatch(collection2, gametestinfo.getTest().batch(), rotation.ordinal());
            collection1.add(gametestbatch);
         }
      }

      StructureGridSpawner structuregridspawner = new StructureGridSpawner(blockpos, 10, true);
      GameTestRunner gametestrunner = GameTestRunner.Builder.fromBatches(collection1, serverlevel)
         .batcher(GameTestBatchFactory.fromGameTestInfo(100))
         .newStructureSpawner(structuregridspawner)
         .existingStructureSpawner(structuregridspawner)
         .haltOnError()
         .clearBetweenBatches()
         .build();
      return trackAndStartRunner(commandsourcestack, gametestrunner);
   }

   private static int run(TestFinder p_396663_, RetryOptions p_394304_, int p_391302_, int p_395268_) {
      stopTests();
      CommandSourceStack commandsourcestack = p_396663_.source();
      ServerLevel serverlevel = commandsourcestack.getLevel();
      BlockPos blockpos = createTestPositionAround(commandsourcestack);
      Collection<GameTestInfo> collection = Stream.concat(
            toGameTestInfos(commandsourcestack, p_394304_, p_396663_), toGameTestInfo(commandsourcestack, p_394304_, p_396663_, p_391302_)
         )
         .toList();
      if (collection.isEmpty()) {
         commandsourcestack.sendSuccess(() -> Component.translatable("commands.test.no_tests"), false);
         return 0;
      } else {
         FailedTestTracker.forgetFailedTests();
         commandsourcestack.sendSuccess(() -> Component.translatable("commands.test.run.running", collection.size()), false);
         GameTestRunner gametestrunner = GameTestRunner.Builder.fromInfo(collection, serverlevel)
            .newStructureSpawner(new StructureGridSpawner(blockpos, p_395268_, false))
            .build();
         return trackAndStartRunner(commandsourcestack, gametestrunner);
      }
   }

   private static int locate(TestFinder p_392659_) throws CommandSyntaxException {
      p_392659_.source().sendSystemMessage(Component.translatable("commands.test.locate.started"));
      MutableInt mutableint = new MutableInt(0);
      BlockPos blockpos = BlockPos.containing(p_392659_.source().getPosition());
      p_392659_.findTestPos()
         .forEach(
            p_389808_ -> {
               if (p_392659_.source().getLevel().getBlockEntity(p_389808_) instanceof TestInstanceBlockEntity testinstanceblockentity) {
                  Direction direction = testinstanceblockentity.getRotation().rotate(Direction.NORTH);
                  BlockPos $$8 = testinstanceblockentity.getBlockPos().relative(direction, 2);
                  int $$9 = (int)direction.getOpposite().toYRot();
                  String $$10 = String.format(Locale.ROOT, "/tp @s %d %d %d %d 0", $$8.getX(), $$8.getY(), $$8.getZ(), $$9);
                  int $$11 = blockpos.getX() - p_389808_.getX();
                  int $$12 = blockpos.getZ() - p_389808_.getZ();
                  int $$13 = Mth.floor(Mth.sqrt($$11 * $$11 + $$12 * $$12));
                  MutableComponent $$14 = ComponentUtils.wrapInSquareBrackets(
                        Component.translatable("chat.coordinates", p_389808_.getX(), p_389808_.getY(), p_389808_.getZ())
                     )
                     .withStyle(
                        p_389833_ -> p_389833_.withColor(ChatFormatting.GREEN)
                           .withClickEvent(new ClickEvent.SuggestCommand($$10))
                           .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                     );
                  p_392659_.source().sendSuccess(() -> Component.translatable("commands.test.locate.found", $$14, $$13), false);
                  mutableint.increment();
               }
            }
         );
      int i = mutableint.intValue();
      if (i == 0) {
         throw NO_TEST_INSTANCES.create();
      }

      p_392659_.source().sendSuccess(() -> Component.translatable("commands.test.locate.done", i), true);
      return i;
   }

   private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
      ArgumentBuilder<CommandSourceStack, ?> p_331571_,
      InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> p_395261_,
      Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> p_335923_
   ) {
      return p_331571_.executes(p_389857_ -> run(p_395261_.apply(p_389857_), RetryOptions.noRetries(), 0, 8))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("numberOfTimes", IntegerArgumentType.integer(0))
                  .executes(
                     p_389822_ -> run(p_395261_.apply(p_389822_), new RetryOptions(IntegerArgumentType.getInteger(p_389822_, "numberOfTimes"), false), 0, 8)
                  ))
               .then(
                  p_335923_.apply(
                     Commands.argument("untilFailed", BoolArgumentType.bool())
                        .executes(
                           p_389810_ -> run(
                              p_395261_.apply(p_389810_),
                              new RetryOptions(IntegerArgumentType.getInteger(p_389810_, "numberOfTimes"), BoolArgumentType.getBool(p_389810_, "untilFailed")),
                              0,
                              8
                           )
                        )
                  )
               )
         );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
      ArgumentBuilder<CommandSourceStack, ?> p_335642_, InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> p_396652_
   ) {
      return runWithRetryOptions(p_335642_, p_396652_, p_325997_ -> p_325997_);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptionsAndBuildInfo(
      ArgumentBuilder<CommandSourceStack, ?> p_328748_, InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> p_395005_
   ) {
      return runWithRetryOptions(
         p_328748_,
         p_395005_,
         p_325993_ -> p_325993_.then(
            ((RequiredArgumentBuilder)Commands.argument("rotationSteps", IntegerArgumentType.integer())
                  .executes(
                     p_389839_ -> run(
                        p_395005_.apply(p_389839_),
                        new RetryOptions(IntegerArgumentType.getInteger(p_389839_, "numberOfTimes"), BoolArgumentType.getBool(p_389839_, "untilFailed")),
                        IntegerArgumentType.getInteger(p_389839_, "rotationSteps"),
                        8
                     )
                  ))
               .then(
                  Commands.argument("testsPerRow", IntegerArgumentType.integer())
                     .executes(
                        p_389844_ -> run(
                           p_395005_.apply(p_389844_),
                           new RetryOptions(IntegerArgumentType.getInteger(p_389844_, "numberOfTimes"), BoolArgumentType.getBool(p_389844_, "untilFailed")),
                           IntegerArgumentType.getInteger(p_389844_, "rotationSteps"),
                           IntegerArgumentType.getInteger(p_389844_, "testsPerRow")
                        )
                     )
               )
         )
      );
   }

   public static void register(CommandDispatcher<CommandSourceStack> p_395323_, CommandBuildContext p_393183_) {
      ArgumentBuilder<CommandSourceStack, ?> argumentbuilder = runWithRetryOptionsAndBuildInfo(
         Commands.argument("onlyRequiredTests", BoolArgumentType.bool()),
         p_389854_ -> TestFinder.builder().failedTests(p_389854_, BoolArgumentType.getBool(p_389854_, "onlyRequiredTests"))
      );
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                               "test"
                                                            )
                                                            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                                                         .then(
                                                            Commands.literal("run")
                                                               .then(
                                                                  runWithRetryOptionsAndBuildInfo(
                                                                     Commands.argument(
                                                                        "tests", ResourceSelectorArgument.resourceSelector(p_393183_, Registries.TEST_INSTANCE)
                                                                     ),
                                                                     p_405078_ -> TestFinder.builder()
                                                                        .byResourceSelection(
                                                                           p_405078_, ResourceSelectorArgument.getSelectedResources(p_405078_, "tests")
                                                                        )
                                                                  )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("runmultiple")
                                                            .then(
                                                               ((RequiredArgumentBuilder)Commands.argument(
                                                                        "tests", ResourceSelectorArgument.resourceSelector(p_393183_, Registries.TEST_INSTANCE)
                                                                     )
                                                                     .executes(
                                                                        p_405077_ -> run(
                                                                           TestFinder.builder()
                                                                              .byResourceSelection(
                                                                                 p_405077_, ResourceSelectorArgument.getSelectedResources(p_405077_, "tests")
                                                                              ),
                                                                           RetryOptions.noRetries(),
                                                                           0,
                                                                           8
                                                                        )
                                                                     ))
                                                                  .then(
                                                                     Commands.argument("amount", IntegerArgumentType.integer())
                                                                        .executes(
                                                                           p_405080_ -> run(
                                                                              TestFinder.builder()
                                                                                 .createMultipleCopies(IntegerArgumentType.getInteger(p_405080_, "amount"))
                                                                                 .byResourceSelection(
                                                                                    p_405080_,
                                                                                    ResourceSelectorArgument.getSelectedResources(p_405080_, "tests")
                                                                                 ),
                                                                              RetryOptions.noRetries(),
                                                                              0,
                                                                              8
                                                                           )
                                                                        )
                                                                  )
                                                            )
                                                      ))
                                                   .then(runWithRetryOptions(Commands.literal("runthese"), TestFinder.builder()::allNearby)))
                                                .then(runWithRetryOptions(Commands.literal("runclosest"), TestFinder.builder()::nearest)))
                                             .then(runWithRetryOptions(Commands.literal("runthat"), TestFinder.builder()::lookedAt)))
                                          .then(
                                             runWithRetryOptionsAndBuildInfo(
                                                Commands.literal("runfailed").then(argumentbuilder), TestFinder.builder()::failedTests
                                             )
                                          ))
                                       .then(
                                          Commands.literal("verify")
                                             .then(
                                                Commands.argument("tests", ResourceSelectorArgument.resourceSelector(p_393183_, Registries.TEST_INSTANCE))
                                                   .executes(
                                                      p_405079_ -> verify(
                                                         TestFinder.builder()
                                                            .byResourceSelection(p_405079_, ResourceSelectorArgument.getSelectedResources(p_405079_, "tests"))
                                                      )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("locate")
                                          .then(
                                             Commands.argument("tests", ResourceSelectorArgument.resourceSelector(p_393183_, Registries.TEST_INSTANCE))
                                                .executes(
                                                   p_405076_ -> locate(
                                                      TestFinder.builder()
                                                         .byResourceSelection(p_405076_, ResourceSelectorArgument.getSelectedResources(p_405076_, "tests"))
                                                   )
                                                )
                                          )
                                    ))
                                 .then(Commands.literal("resetclosest").executes(p_389837_ -> reset(TestFinder.builder().nearest(p_389837_)))))
                              .then(Commands.literal("resetthese").executes(p_389842_ -> reset(TestFinder.builder().allNearby(p_389842_)))))
                           .then(Commands.literal("resetthat").executes(p_389840_ -> reset(TestFinder.builder().lookedAt(p_389840_)))))
                        .then(Commands.literal("clearthat").executes(p_389813_ -> clear(TestFinder.builder().lookedAt(p_389813_)))))
                     .then(Commands.literal("clearthese").executes(p_389845_ -> clear(TestFinder.builder().allNearby(p_389845_)))))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("clearall").executes(p_389796_ -> clear(TestFinder.builder().radius(p_389796_, 250))))
                        .then(
                           Commands.argument("radius", IntegerArgumentType.integer())
                              .executes(
                                 p_389820_ -> clear(
                                    TestFinder.builder().radius(p_389820_, Mth.clamp(IntegerArgumentType.getInteger(p_389820_, "radius"), 0, 1024))
                                 )
                              )
                        )
                  ))
               .then(Commands.literal("stop").executes(p_326006_ -> stopTests())))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("pos").executes(p_128023_ -> showPos((CommandSourceStack)p_128023_.getSource(), "pos")))
                  .then(
                     Commands.argument("var", StringArgumentType.word())
                        .executes(p_128021_ -> showPos((CommandSourceStack)p_128021_.getSource(), StringArgumentType.getString(p_128021_, "var")))
                  )
            ))
         .then(
            Commands.literal("create")
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                        .suggests(TestCommand::suggestTestFunction)
                        .executes(
                           p_448759_ -> createNewStructure((CommandSourceStack)p_448759_.getSource(), IdentifierArgument.getId(p_448759_, "id"), 5, 5, 5)
                        ))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("width", IntegerArgumentType.integer())
                              .executes(
                                 p_448758_ -> createNewStructure(
                                    (CommandSourceStack)p_448758_.getSource(),
                                    IdentifierArgument.getId(p_448758_, "id"),
                                    IntegerArgumentType.getInteger(p_448758_, "width"),
                                    IntegerArgumentType.getInteger(p_448758_, "width"),
                                    IntegerArgumentType.getInteger(p_448758_, "width")
                                 )
                              ))
                           .then(
                              Commands.argument("height", IntegerArgumentType.integer())
                                 .then(
                                    Commands.argument("depth", IntegerArgumentType.integer())
                                       .executes(
                                          p_448757_ -> createNewStructure(
                                             (CommandSourceStack)p_448757_.getSource(),
                                             IdentifierArgument.getId(p_448757_, "id"),
                                             IntegerArgumentType.getInteger(p_448757_, "width"),
                                             IntegerArgumentType.getInteger(p_448757_, "height"),
                                             IntegerArgumentType.getInteger(p_448757_, "depth")
                                          )
                                       )
                                 )
                           )
                     )
               )
         );
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         literalargumentbuilder = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)literalargumentbuilder.then(
                     Commands.literal("export")
                        .then(
                           Commands.argument("test", ResourceArgument.resource(p_393183_, Registries.TEST_INSTANCE))
                              .executes(
                                 p_389829_ -> exportTestStructure(
                                    (CommandSourceStack)p_389829_.getSource(), ResourceArgument.getResource(p_389829_, "test", Registries.TEST_INSTANCE)
                                 )
                              )
                        )
                  ))
                  .then(Commands.literal("exportclosest").executes(p_389846_ -> export(TestFinder.builder().nearest(p_389846_)))))
               .then(Commands.literal("exportthese").executes(p_389818_ -> export(TestFinder.builder().allNearby(p_389818_)))))
            .then(Commands.literal("exportthat").executes(p_389802_ -> export(TestFinder.builder().lookedAt(p_389802_))));
      }

      p_395323_.register(literalargumentbuilder);
   }

   public static CompletableFuture<Suggestions> suggestTestFunction(CommandContext<CommandSourceStack> p_392312_, SuggestionsBuilder p_393345_) {
      Stream<String> stream = ((CommandSourceStack)p_392312_.getSource())
         .registryAccess()
         .lookupOrThrow(Registries.TEST_FUNCTION)
         .listElements()
         .map(Holder::getRegisteredName);
      return SharedSuggestionProvider.suggest(stream, p_393345_);
   }

   private static int resetGameTestInfo(CommandSourceStack p_394479_, GameTestInfo p_331593_) {
      TestInstanceBlockEntity testinstanceblockentity = p_331593_.getTestInstanceBlockEntity();
      testinstanceblockentity.resetTest(p_394479_::sendSystemMessage);
      return 1;
   }

   private static Stream<GameTestInfo> toGameTestInfos(CommandSourceStack p_329247_, RetryOptions p_336246_, TestPosFinder p_392271_) {
      return p_392271_.findTestPos().map(p_389850_ -> createGameTestInfo(p_389850_, p_329247_, p_336246_)).flatMap(Optional::stream);
   }

   private static Stream<GameTestInfo> toGameTestInfo(CommandSourceStack p_330917_, RetryOptions p_332428_, TestInstanceFinder p_393793_, int p_327985_) {
      return p_393793_.findTests()
         .filter(p_448761_ -> verifyStructureExists(p_330917_, p_448761_.value().structure()))
         .map(
            p_389828_ -> new GameTestInfo(
               (Holder.Reference<GameTestInstance>)p_389828_, StructureUtils.getRotationForRotationSteps(p_327985_), p_330917_.getLevel(), p_332428_
            )
         );
   }

   private static Optional<GameTestInfo> createGameTestInfo(BlockPos p_332856_, CommandSourceStack p_393532_, RetryOptions p_330368_) {
      ServerLevel serverlevel = p_393532_.getLevel();
      if (serverlevel.getBlockEntity(p_332856_) instanceof TestInstanceBlockEntity testinstanceblockentity) {
         Optional<Holder.Reference<GameTestInstance>> optional = testinstanceblockentity.test()
            .flatMap(p_393532_.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE)::get);
         if (optional.isEmpty()) {
            p_393532_.sendFailure(Component.translatable("commands.test.error.non_existant_test", testinstanceblockentity.getTestName()));
            return Optional.empty();
         } else {
            Holder.Reference<GameTestInstance> reference = optional.get();
            GameTestInfo gametestinfo = new GameTestInfo(reference, testinstanceblockentity.getRotation(), serverlevel, p_330368_);
            gametestinfo.setTestBlockPos(p_332856_);
            return !verifyStructureExists(p_393532_, gametestinfo.getStructure()) ? Optional.empty() : Optional.of(gametestinfo);
         }
      } else {
         p_393532_.sendFailure(
            Component.translatable("commands.test.error.test_instance_not_found.position", p_332856_.getX(), p_332856_.getY(), p_332856_.getZ())
         );
         return Optional.empty();
      }
   }

   private static int createNewStructure(CommandSourceStack p_127968_, Identifier p_457791_, int p_127970_, int p_127971_, int p_127972_) throws CommandSyntaxException {
      if (p_127970_ <= 48 && p_127971_ <= 48 && p_127972_ <= 48) {
         ServerLevel serverlevel = p_127968_.getLevel();
         BlockPos blockpos = createTestPositionAround(p_127968_);
         TestInstanceBlockEntity testinstanceblockentity = StructureUtils.createNewEmptyTest(
            p_457791_, blockpos, new Vec3i(p_127970_, p_127971_, p_127972_), Rotation.NONE, serverlevel
         );
         BlockPos blockpos1 = testinstanceblockentity.getStructurePos();
         BlockPos blockpos2 = blockpos1.offset(p_127970_ - 1, 0, p_127972_ - 1);
         BlockPos.betweenClosedStream(blockpos1, blockpos2).forEach(p_405082_ -> serverlevel.setBlockAndUpdate(p_405082_, Blocks.BEDROCK.defaultBlockState()));
         p_127968_.sendSuccess(() -> Component.translatable("commands.test.create.success", testinstanceblockentity.getTestName()), true);
         return 1;
      } else {
         throw TOO_LARGE.create(48);
      }
   }

   private static int showPos(CommandSourceStack p_127960_, String p_127961_) throws CommandSyntaxException {
      ServerPlayer serverplayer = p_127960_.getPlayerOrException();
      BlockHitResult blockhitresult = (BlockHitResult)serverplayer.pick(10.0, 1.0F, false);
      BlockPos blockpos = blockhitresult.getBlockPos();
      ServerLevel serverlevel = p_127960_.getLevel();
      Optional<BlockPos> optional = StructureUtils.findTestContainingPos(blockpos, 15, serverlevel);
      if (optional.isEmpty()) {
         optional = StructureUtils.findTestContainingPos(blockpos, 250, serverlevel);
      }

      if (optional.isEmpty()) {
         throw NO_TEST_CONTAINING.create(blockpos.getX(), blockpos.getY(), blockpos.getZ());
      } else if (serverlevel.getBlockEntity(optional.get()) instanceof TestInstanceBlockEntity testinstanceblockentity) {
         BlockPos blockpos2 = testinstanceblockentity.getStructurePos();
         BlockPos blockpos1 = blockpos.subtract(blockpos2);
         String $$11 = blockpos1.getX() + ", " + blockpos1.getY() + ", " + blockpos1.getZ();
         String $$12 = testinstanceblockentity.getTestName().getString();
         MutableComponent $$13 = Component.translatable("commands.test.coordinates", blockpos1.getX(), blockpos1.getY(), blockpos1.getZ())
            .setStyle(
               Style.EMPTY
                  .withBold(true)
                  .withColor(ChatFormatting.GREEN)
                  .withHoverEvent(new HoverEvent.ShowText(Component.translatable("commands.test.coordinates.copy")))
                  .withClickEvent(new ClickEvent.CopyToClipboard("final BlockPos " + p_127961_ + " = new BlockPos(" + $$11 + ");"))
            );
         p_127960_.sendSuccess(() -> Component.translatable("commands.test.relative_position", $$12, $$13), false);
         serverplayer.connection.send(new ClientboundGameTestHighlightPosPacket(blockpos, blockpos1));
         return 1;
      } else {
         throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
      }
   }

   private static int stopTests() {
      GameTestTicker.SINGLETON.clear();
      return 1;
   }

   public static int trackAndStartRunner(CommandSourceStack p_333535_, GameTestRunner p_333430_) {
      p_333430_.addListener(new TestCommand.TestBatchSummaryDisplayer(p_333535_));
      MultipleTestTracker multipletesttracker = new MultipleTestTracker(p_333430_.getTestInfos());
      multipletesttracker.addListener(new TestCommand.TestSummaryDisplayer(p_333535_, multipletesttracker));
      multipletesttracker.addFailureListener(p_389841_ -> FailedTestTracker.rememberFailedTest(p_389841_.getTestHolder()));
      p_333430_.start();
      return 1;
   }

   private static int exportTestStructure(CommandSourceStack p_128011_, Holder<GameTestInstance> p_392480_) {
      return !TestInstanceBlockEntity.export(p_128011_.getLevel(), p_392480_.value().structure(), p_128011_::sendSystemMessage) ? 0 : 1;
   }

   private static boolean verifyStructureExists(CommandSourceStack p_395581_, Identifier p_456651_) {
      if (p_395581_.getLevel().getStructureManager().get(p_456651_).isEmpty()) {
         p_395581_.sendFailure(Component.translatable("commands.test.error.structure_not_found", Component.translationArg(p_456651_)));
         return false;
      } else {
         return true;
      }
   }

   private static BlockPos createTestPositionAround(CommandSourceStack p_313084_) {
      BlockPos blockpos = BlockPos.containing(p_313084_.getPosition());
      int i = p_313084_.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, blockpos).getY();
      return new BlockPos(blockpos.getX(), i, blockpos.getZ() + 3);
   }

   record TestBatchSummaryDisplayer(CommandSourceStack source) implements GameTestBatchListener {
      @Override
      public void testBatchStarting(GameTestBatch p_327831_) {
         this.source
            .sendSuccess(() -> Component.translatable("commands.test.batch.starting", p_327831_.environment().getRegisteredName(), p_327831_.index()), true);
      }

      @Override
      public void testBatchFinished(GameTestBatch p_335734_) {
      }
   }

   public record TestSummaryDisplayer(CommandSourceStack source, MultipleTestTracker tracker) implements GameTestListener {
      @Override
      public void testStructureLoaded(GameTestInfo p_128064_) {
      }

      @Override
      public void testPassed(GameTestInfo p_177797_, GameTestRunner p_333026_) {
         this.showTestSummaryIfAllDone();
      }

      @Override
      public void testFailed(GameTestInfo p_128066_, GameTestRunner p_333809_) {
         this.showTestSummaryIfAllDone();
      }

      @Override
      public void testAddedForRerun(GameTestInfo p_328539_, GameTestInfo p_335500_, GameTestRunner p_328503_) {
         this.tracker.addTestToTrack(p_335500_);
      }

      private void showTestSummaryIfAllDone() {
         if (this.tracker.isDone()) {
            this.source.sendSuccess(() -> Component.translatable("commands.test.summary", this.tracker.getTotalCount()).withStyle(ChatFormatting.WHITE), true);
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
