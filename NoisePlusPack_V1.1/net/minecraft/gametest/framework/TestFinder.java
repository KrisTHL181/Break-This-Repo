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

public class TestFinder implements TestInstanceFinder, TestPosFinder {
   static final TestInstanceFinder NO_FUNCTIONS = Stream::empty;
   static final TestPosFinder NO_STRUCTURES = Stream::empty;
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

   TestFinder(CommandSourceStack p_332130_, TestInstanceFinder p_396070_, TestPosFinder p_391434_) {
      this.source = p_332130_;
      this.testInstanceFinder = p_396070_;
      this.testPosFinder = p_391434_;
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
         this.testFinderWrapper = p_333647_ -> p_333647_;
         this.structureBlockPosFinderWrapper = p_327811_ -> p_327811_;
      }

      private Builder(UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> p_395398_, UnaryOperator<Supplier<Stream<BlockPos>>> p_392467_) {
         this.testFinderWrapper = p_395398_;
         this.structureBlockPosFinderWrapper = p_392467_;
      }

      public TestFinder.Builder createMultipleCopies(int p_329806_) {
         return new TestFinder.Builder(createCopies(p_329806_), createCopies(p_329806_));
      }

      private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(int p_334571_) {
         return p_448771_ -> {
            List<Q> list = new LinkedList<>();
            List<Q> list1 = ((Stream)p_448771_.get()).toList();

            for (int i = 0; i < p_334571_; i++) {
               list.addAll(list1);
            }

            return list::stream;
         };
      }

      private TestFinder build(CommandSourceStack p_334153_, TestInstanceFinder p_396511_, TestPosFinder p_391471_) {
         return new TestFinder(
            p_334153_, this.testFinderWrapper.apply(p_396511_::findTests)::get, this.structureBlockPosFinderWrapper.apply(p_391471_::findTestPos)::get
         );
      }

      public TestFinder radius(CommandContext<CommandSourceStack> p_330481_, int p_334173_) {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_330481_.getSource();
         BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
         return this.build(commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(blockpos, p_334173_, commandsourcestack.getLevel()));
      }

      public TestFinder nearest(CommandContext<CommandSourceStack> p_332654_) {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_332654_.getSource();
         BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
         return this.build(
            commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findNearestTest(blockpos, 15, commandsourcestack.getLevel()).stream()
         );
      }

      public TestFinder allNearby(CommandContext<CommandSourceStack> p_335428_) {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_335428_.getSource();
         BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
         return this.build(commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(blockpos, 250, commandsourcestack.getLevel()));
      }

      public TestFinder lookedAt(CommandContext<CommandSourceStack> p_328071_) {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_328071_.getSource();
         return this.build(
            commandsourcestack,
            TestFinder.NO_FUNCTIONS,
            () -> StructureUtils.lookedAtTestPos(
               BlockPos.containing(commandsourcestack.getPosition()), commandsourcestack.getPlayer().getCamera(), commandsourcestack.getLevel()
            )
         );
      }

      public TestFinder failedTests(CommandContext<CommandSourceStack> p_331687_, boolean p_393883_) {
         return this.build(
            (CommandSourceStack)p_331687_.getSource(),
            () -> FailedTestTracker.getLastFailedTests().filter(p_389864_ -> !p_393883_ || p_389864_.value().required()),
            TestFinder.NO_STRUCTURES
         );
      }

      public TestFinder byResourceSelection(CommandContext<CommandSourceStack> p_397446_, Collection<Holder.Reference<GameTestInstance>> p_395283_) {
         return this.build((CommandSourceStack)p_397446_.getSource(), p_395283_::stream, TestFinder.NO_STRUCTURES);
      }

      public TestFinder failedTests(CommandContext<CommandSourceStack> p_332736_) {
         return this.failedTests(p_332736_, false);
      }
   }
}
