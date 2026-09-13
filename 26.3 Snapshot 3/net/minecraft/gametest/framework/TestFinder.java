package net.minecraft.gametest.framework;

import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class TestFinder implements TestInstanceFinder, TestPosFinder {
   private static final TestInstanceFinder NO_FUNCTIONS = Stream::empty;
   private static final TestPosFinder NO_STRUCTURES = Stream::empty;
   private final TestInstanceFinder testInstanceFinder;
   private final TestPosFinder testPosFinder;
   private final CommandSourceStack source;

   @Override
   public Stream<BlockPos> findTestPos() {
      return this.testPosFinder.findTestPos();
   }

   public static TestFinder.Builder builder() {
      return new TestFinder.Builder();
   }

   private TestFinder(final CommandSourceStack source, final TestInstanceFinder testInstanceFinder, final TestPosFinder testPosFinder) {
      this.source = source;
      this.testInstanceFinder = testInstanceFinder;
      this.testPosFinder = testPosFinder;
   }

   public CommandSourceStack source() {
      return this.source;
   }

   @Override
   public Stream<Holder.Reference<GameTestInstance>> findTests() {
      return this.testInstanceFinder.findTests();
   }

   public static class Builder {
      private final UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper;
      private final UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper;

      public Builder() {
         this.testFinderWrapper = f -> f;
         this.structureBlockPosFinderWrapper = f -> f;
      }

      private Builder(
         final UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper,
         final UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper
      ) {
         this.testFinderWrapper = testFinderWrapper;
         this.structureBlockPosFinderWrapper = structureBlockPosFinderWrapper;
      }

      public TestFinder.Builder createMultipleCopies(final int amount) {
         return new TestFinder.Builder(createCopies(amount), createCopies(amount));
      }

      private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(final int amount) {
         return source -> {
            List<Q> copyList = new LinkedList<>();
            List<Q> sourceList = ((Stream)source.get()).toList();

            for (int i = 0; i < amount; i++) {
               copyList.addAll(sourceList);
            }

            return copyList::stream;
         };
      }

      public static ResourceKey<Level> levelForDimension(final TestEnvironmentDefinition<?> definition) {
         return switch (definition) {
            case TestEnvironmentDefinition.Dimension dimension -> {
               switch (dimension.type()) {
                  case OVERWORLD:
                     yield Level.OVERWORLD;
                  case NETHER:
                     yield Level.NETHER;
                  case END:
                     yield Level.END;
                  default:
                     throw new MatchException(null, null);
               }
            }
            case TestEnvironmentDefinition.AllOf allOf -> (ResourceKey)allOf.definitions()
               .stream()
               .map(h -> levelForDimension(h.value()))
               .filter(key -> key != Level.OVERWORLD)
               .findFirst()
               .orElse(Level.OVERWORLD);
            default -> Level.OVERWORLD;
         };
      }

      private static CommandSourceStack resolveTargetSource(final CommandSourceStack source, final List<Holder.Reference<GameTestInstance>> tests) {
         ResourceKey<Level> requestedLevel = tests.stream()
            .map(test -> levelForDimension(test.value().batch().value()))
            .findFirst()
            .orElse(source.getLevel().dimension());
         ServerLevel targetLevel = source.getServer().getLevel(requestedLevel);
         return targetLevel != null ? source.withLevel(targetLevel) : source;
      }

      private TestFinder build(final CommandSourceStack source, final TestInstanceFinder testInstanceFinder, final TestPosFinder testPosFinder) {
         List<Holder.Reference<GameTestInstance>> selectedTests = testInstanceFinder.findTests().toList();
         CommandSourceStack targetSource = resolveTargetSource(source, selectedTests);
         return new TestFinder(
            targetSource, this.testFinderWrapper.apply(selectedTests::stream)::get, this.structureBlockPosFinderWrapper.apply(testPosFinder::findTestPos)::get
         );
      }

      public TestFinder radius(final CommandContext<CommandSourceStack> sourceStack, final int radius) {
         CommandSourceStack source = (CommandSourceStack)sourceStack.getSource();
         BlockPos pos = BlockPos.containing(source.getPosition());
         return this.build(source, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(pos, radius, source.getLevel()));
      }

      public TestFinder nearest(final CommandContext<CommandSourceStack> sourceStack) {
         CommandSourceStack source = (CommandSourceStack)sourceStack.getSource();
         BlockPos pos = BlockPos.containing(source.getPosition());
         return this.build(source, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findNearestTest(pos, 15, source.getLevel()).stream());
      }

      public TestFinder allNearby(final CommandContext<CommandSourceStack> sourceStack) {
         CommandSourceStack source = (CommandSourceStack)sourceStack.getSource();
         BlockPos pos = BlockPos.containing(source.getPosition());
         return this.build(source, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(pos, 250, source.getLevel()));
      }

      public TestFinder lookedAt(final CommandContext<CommandSourceStack> sourceStack) {
         CommandSourceStack source = (CommandSourceStack)sourceStack.getSource();
         return this.build(
            source,
            TestFinder.NO_FUNCTIONS,
            () -> StructureUtils.lookedAtTestPos(BlockPos.containing(source.getPosition()), source.getPlayer().getCamera(), source.getLevel())
         );
      }

      public TestFinder failedTests(final CommandContext<CommandSourceStack> sourceStack, final boolean onlyRequiredTests) {
         return this.build(
            (CommandSourceStack)sourceStack.getSource(),
            () -> FailedTestTracker.getLastFailedTests().filter(test -> !onlyRequiredTests || test.value().required()),
            TestFinder.NO_STRUCTURES
         );
      }

      public TestFinder byResourceSelection(final CommandContext<CommandSourceStack> sourceStack, final Collection<Holder.Reference<GameTestInstance>> holders) {
         return this.build((CommandSourceStack)sourceStack.getSource(), holders::stream, TestFinder.NO_STRUCTURES);
      }

      public TestFinder failedTests(final CommandContext<CommandSourceStack> sourceStack) {
         return this.failedTests(sourceStack, false);
      }
   }
}
