package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CloneCommands {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(Component.translatable("commands.clone.overlap"));
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (max, count) -> Component.translatableEscape("commands.clone.toobig", max, count)
   );
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
   public static final Predicate<BlockInWorld> FILTER_AIR = b -> !b.getState().isAir();

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clone")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(beginEndDestinationAndModeSuffix(context, c -> ((CommandSourceStack)c.getSource()).getLevel())))
            .then(
               Commands.literal("from")
                  .then(
                     Commands.argument("sourceDimension", DimensionArgument.dimension())
                        .then(beginEndDestinationAndModeSuffix(context, c -> DimensionArgument.getDimension(c, "sourceDimension")))
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(
      final CommandBuildContext context, final InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> fromDimension
   ) {
      return Commands.argument("begin", BlockPosArgument.blockPos())
         .then(
            ((RequiredArgumentBuilder)Commands.argument("end", BlockPosArgument.blockPos())
                  .then(destinationAndStrictSuffix(context, fromDimension, c -> ((CommandSourceStack)c.getSource()).getLevel())))
               .then(
                  Commands.literal("to")
                     .then(
                        Commands.argument("targetDimension", DimensionArgument.dimension())
                           .then(destinationAndStrictSuffix(context, fromDimension, c -> DimensionArgument.getDimension(c, "targetDimension")))
                     )
               )
         );
   }

   private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(
      final CommandContext<CommandSourceStack> context, final ServerLevel level, final String positionArgument
   ) throws CommandSyntaxException {
      BlockPos blockPos = BlockPosArgument.getLoadedBlockPos(context, level, positionArgument);
      return new CloneCommands.DimensionAndPosition(level, blockPos);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> destinationAndStrictSuffix(
      final CommandBuildContext context,
      final InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> fromDimension,
      final InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> toDimension
   ) {
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> beginPos = c -> getLoadedDimensionAndPosition(
         c, fromDimension.apply(c), "begin"
      );
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> endPos = c -> getLoadedDimensionAndPosition(
         c, fromDimension.apply(c), "end"
      );
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> destinationPos = c -> getLoadedDimensionAndPosition(
         c, toDimension.apply(c), "destination"
      );
      return modeSuffix(context, beginPos, endPos, destinationPos, false, Commands.argument("destination", BlockPosArgument.blockPos()))
         .then(modeSuffix(context, beginPos, endPos, destinationPos, true, Commands.literal("strict")));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> modeSuffix(
      final CommandBuildContext context,
      final InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> beginPos,
      final InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> endPos,
      final InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> destinationPos,
      final boolean strict,
      final ArgumentBuilder<CommandSourceStack, ?> builder
   ) {
      return builder.executes(
            c -> clone(
               (CommandSourceStack)c.getSource(), beginPos.apply(c), endPos.apply(c), destinationPos.apply(c), b -> true, CloneCommands.Mode.NORMAL, strict
            )
         )
         .then(wrapWithCloneMode(beginPos, endPos, destinationPos, c -> b -> true, strict, Commands.literal("replace")))
         .then(wrapWithCloneMode(beginPos, endPos, destinationPos, c -> FILTER_AIR, strict, Commands.literal("masked")))
         .then(
            Commands.literal("filtered")
               .then(
                  wrapWithCloneMode(
                     beginPos,
                     endPos,
                     destinationPos,
                     c -> BlockPredicateArgument.getBlockPredicate(c, "filter"),
                     strict,
                     Commands.argument("filter", BlockPredicateArgument.blockPredicate(context))
                  )
               )
         );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(
      final InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> beginPos,
      final InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> endPos,
      final InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> destinationPos,
      final InCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> filter,
      final boolean strict,
      final ArgumentBuilder<CommandSourceStack, ?> builder
   ) {
      return builder.executes(
            c -> clone(
               (CommandSourceStack)c.getSource(),
               beginPos.apply(c),
               endPos.apply(c),
               destinationPos.apply(c),
               filter.apply(c),
               CloneCommands.Mode.NORMAL,
               strict
            )
         )
         .then(
            Commands.literal("force")
               .executes(
                  c -> clone(
                     (CommandSourceStack)c.getSource(),
                     beginPos.apply(c),
                     endPos.apply(c),
                     destinationPos.apply(c),
                     filter.apply(c),
                     CloneCommands.Mode.FORCE,
                     strict
                  )
               )
         )
         .then(
            Commands.literal("move")
               .executes(
                  c -> clone(
                     (CommandSourceStack)c.getSource(),
                     beginPos.apply(c),
                     endPos.apply(c),
                     destinationPos.apply(c),
                     filter.apply(c),
                     CloneCommands.Mode.MOVE,
                     strict
                  )
               )
         )
         .then(
            Commands.literal("normal")
               .executes(
                  c -> clone(
                     (CommandSourceStack)c.getSource(),
                     beginPos.apply(c),
                     endPos.apply(c),
                     destinationPos.apply(c),
                     filter.apply(c),
                     CloneCommands.Mode.NORMAL,
                     strict
                  )
               )
         );
   }

   private static int clone(
      final CommandSourceStack source,
      final CloneCommands.DimensionAndPosition startPosAndDimension,
      final CloneCommands.DimensionAndPosition endPosAndDimension,
      final CloneCommands.DimensionAndPosition destPosAndDimension,
      final Predicate<BlockInWorld> predicate,
      final CloneCommands.Mode mode,
      final boolean strict
   ) throws CommandSyntaxException {
      BlockPos startPos = startPosAndDimension.position();
      BlockPos endPos = endPosAndDimension.position();
      BoundingBox from = BoundingBox.fromCorners(startPos, endPos);
      BlockPos destPos = destPosAndDimension.position();
      BlockPos destEndPos = destPos.offset(from.getLength());
      BoundingBox destination = BoundingBox.fromCorners(destPos, destEndPos);
      ServerLevel fromDimension = startPosAndDimension.dimension();
      ServerLevel toDimension = destPosAndDimension.dimension();
      if (!mode.canOverlap() && fromDimension == toDimension && destination.intersects(from)) {
         throw ERROR_OVERLAP.create();
      }

      long area = (long)from.getXSpan() * from.getYSpan() * from.getZSpan();
      int limit = source.getLevel().getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
      if (area > limit) {
         throw ERROR_AREA_TOO_LARGE.create(limit, area);
      }

      if (!fromDimension.hasChunksAt(startPos, endPos) || !toDimension.hasChunksAt(destPos, destEndPos)) {
         throw BlockPosArgument.ERROR_NOT_LOADED.create();
      }

      if (toDimension.isDebug()) {
         throw ERROR_FAILED.create();
      }

      List<CloneCommands.CloneBlockInfo> solidList = Lists.newArrayList();
      List<CloneCommands.CloneBlockInfo> blockEntitiesList = Lists.newArrayList();
      List<CloneCommands.CloneBlockInfo> otherBlocksList = Lists.newArrayList();
      Deque<BlockPos> clearBlocksList = Lists.newLinkedList();
      int count = 0;
      ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER);

      try {
         BlockPos offset = new BlockPos(destination.minX() - from.minX(), destination.minY() - from.minY(), destination.minZ() - from.minZ());

         for (int z = from.minZ(); z <= from.maxZ(); z++) {
            for (int y = from.minY(); y <= from.maxY(); y++) {
               for (int x = from.minX(); x <= from.maxX(); x++) {
                  BlockPos sourcePos = new BlockPos(x, y, z);
                  BlockPos destinationPos = sourcePos.offset(offset);
                  BlockInWorld block = new BlockInWorld(fromDimension, sourcePos, false);
                  BlockState blockState = block.getState();
                  if (predicate.test(block)) {
                     BlockEntity blockEntity = fromDimension.getBlockEntity(sourcePos);
                     if (blockEntity != null) {
                        TagValueOutput output = TagValueOutput.createWithContext(reporter.forChild(blockEntity.problemPath()), source.registryAccess());
                        blockEntity.saveCustomOnly(output);
                        CloneCommands.CloneBlockEntityInfo blockEntityInfo = new CloneCommands.CloneBlockEntityInfo(
                           output.buildResult(), blockEntity.components()
                        );
                        blockEntitiesList.add(
                           new CloneCommands.CloneBlockInfo(destinationPos, blockState, blockEntityInfo, toDimension.getBlockState(destinationPos))
                        );
                        clearBlocksList.addLast(sourcePos);
                     } else if (!blockState.isSolidRender() && !blockState.isCollisionShapeFullBlock(fromDimension, sourcePos)) {
                        otherBlocksList.add(new CloneCommands.CloneBlockInfo(destinationPos, blockState, null, toDimension.getBlockState(destinationPos)));
                        clearBlocksList.addFirst(sourcePos);
                     } else {
                        solidList.add(new CloneCommands.CloneBlockInfo(destinationPos, blockState, null, toDimension.getBlockState(destinationPos)));
                        clearBlocksList.addLast(sourcePos);
                     }
                  }
               }
            }
         }

         int defaultUpdateFlags = 2 | (strict ? 816 : 0);
         if (mode == CloneCommands.Mode.MOVE) {
            for (BlockPos pos : clearBlocksList) {
               fromDimension.setBlock(pos, Blocks.BARRIER.defaultBlockState(), defaultUpdateFlags | 816);
            }

            int standardUpdateFlags = strict ? defaultUpdateFlags : 3;

            for (BlockPos pos : clearBlocksList) {
               fromDimension.setBlock(pos, Blocks.AIR.defaultBlockState(), standardUpdateFlags);
            }
         }

         List<CloneCommands.CloneBlockInfo> blockInfoList = Lists.newArrayList();
         blockInfoList.addAll(solidList);
         blockInfoList.addAll(blockEntitiesList);
         blockInfoList.addAll(otherBlocksList);
         List<CloneCommands.CloneBlockInfo> reverse = Lists.reverse(blockInfoList);

         for (CloneCommands.CloneBlockInfo cloneInfo : reverse) {
            toDimension.setBlock(cloneInfo.pos, Blocks.BARRIER.defaultBlockState(), defaultUpdateFlags | 816);
         }

         for (CloneCommands.CloneBlockInfo cloneInfo : blockInfoList) {
            if (toDimension.setBlock(cloneInfo.pos, cloneInfo.state, defaultUpdateFlags)) {
               count++;
            }
         }

         for (CloneCommands.CloneBlockInfo cloneInfo : blockEntitiesList) {
            BlockEntity newBlockEntity = toDimension.getBlockEntity(cloneInfo.pos);
            if (cloneInfo.blockEntityInfo != null && newBlockEntity != null) {
               newBlockEntity.loadCustomOnly(
                  TagValueInput.create(reporter.forChild(newBlockEntity.problemPath()), toDimension.registryAccess(), cloneInfo.blockEntityInfo.tag)
               );
               newBlockEntity.setComponents(cloneInfo.blockEntityInfo.components);
               newBlockEntity.setChanged();
            }

            toDimension.setBlock(cloneInfo.pos, cloneInfo.state, defaultUpdateFlags);
         }

         if (!strict) {
            for (CloneCommands.CloneBlockInfo cloneInfo : reverse) {
               toDimension.updateNeighboursOnBlockSet(cloneInfo.pos, cloneInfo.previousStateAtDestination);
            }
         }

         toDimension.getBlockTicks().copyAreaFrom(fromDimension.getBlockTicks(), from, offset);
      } catch (Throwable var36) {
         try {
            reporter.close();
         } catch (Throwable var35) {
            var36.addSuppressed(var35);
         }

         throw var36;
      }

      reporter.close();
      if (count == 0) {
         throw ERROR_FAILED.create();
      }

      int finalCount = count;
      source.sendSuccess(() -> Component.translatable("commands.clone.success", finalCount), true);
      return count;
   }

   private record CloneBlockEntityInfo(CompoundTag tag, DataComponentMap components) {
   }

   private record CloneBlockInfo(
      BlockPos pos, BlockState state, CloneCommands.@Nullable CloneBlockEntityInfo blockEntityInfo, BlockState previousStateAtDestination
   ) {
   }

   private record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
   }

   private enum Mode {
      FORCE(true),
      MOVE(true),
      NORMAL(false);

      private final boolean canOverlap;

      Mode(final boolean canOverlap) {
         this.canOverlap = canOverlap;
      }

      public boolean canOverlap() {
         return this.canOverlap;
      }
   }
}
