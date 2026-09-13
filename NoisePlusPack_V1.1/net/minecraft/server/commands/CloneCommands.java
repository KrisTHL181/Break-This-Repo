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
      (p_308640_, p_308641_) -> Component.translatableEscape("commands.clone.toobig", p_308640_, p_308641_)
   );
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
   public static final Predicate<BlockInWorld> FILTER_AIR = p_358579_ -> !p_358579_.getState().isAir();

   public static void register(CommandDispatcher<CommandSourceStack> p_214424_, CommandBuildContext p_214425_) {
      p_214424_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clone")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(beginEndDestinationAndModeSuffix(p_214425_, p_264757_ -> ((CommandSourceStack)p_264757_.getSource()).getLevel())))
            .then(
               Commands.literal("from")
                  .then(
                     Commands.argument("sourceDimension", DimensionArgument.dimension())
                        .then(beginEndDestinationAndModeSuffix(p_214425_, p_264743_ -> DimensionArgument.getDimension(p_264743_, "sourceDimension")))
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(
      CommandBuildContext p_265681_, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> p_396447_
   ) {
      return Commands.argument("begin", BlockPosArgument.blockPos())
         .then(
            ((RequiredArgumentBuilder)Commands.argument("end", BlockPosArgument.blockPos())
                  .then(destinationAndStrictSuffix(p_265681_, p_396447_, p_264751_ -> ((CommandSourceStack)p_264751_.getSource()).getLevel())))
               .then(
                  Commands.literal("to")
                     .then(
                        Commands.argument("targetDimension", DimensionArgument.dimension())
                           .then(destinationAndStrictSuffix(p_265681_, p_396447_, p_264756_ -> DimensionArgument.getDimension(p_264756_, "targetDimension")))
                     )
               )
         );
   }

   private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(
      CommandContext<CommandSourceStack> p_265513_, ServerLevel p_265183_, String p_265511_
   ) throws CommandSyntaxException {
      BlockPos blockpos = BlockPosArgument.getLoadedBlockPos(p_265513_, p_265183_, p_265511_);
      return new CloneCommands.DimensionAndPosition(p_265183_, blockpos);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> destinationAndStrictSuffix(
      CommandBuildContext p_397869_,
      InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> p_396883_,
      InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> p_391564_
   ) {
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> incommandfunction = p_389989_ -> getLoadedDimensionAndPosition(
         p_389989_, p_396883_.apply(p_389989_), "begin"
      );
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> incommandfunction1 = p_389970_ -> getLoadedDimensionAndPosition(
         p_389970_, p_396883_.apply(p_389970_), "end"
      );
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> incommandfunction2 = p_389991_ -> getLoadedDimensionAndPosition(
         p_389991_, p_391564_.apply(p_389991_), "destination"
      );
      return modeSuffix(
            p_397869_, incommandfunction, incommandfunction1, incommandfunction2, false, Commands.argument("destination", BlockPosArgument.blockPos())
         )
         .then(modeSuffix(p_397869_, incommandfunction, incommandfunction1, incommandfunction2, true, Commands.literal("strict")));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> modeSuffix(
      CommandBuildContext p_391431_,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> p_391282_,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> p_397375_,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> p_391494_,
      boolean p_396137_,
      ArgumentBuilder<CommandSourceStack, ?> p_391412_
   ) {
      return p_391412_.executes(
            p_389981_ -> clone(
               (CommandSourceStack)p_389981_.getSource(),
               p_391282_.apply(p_389981_),
               p_397375_.apply(p_389981_),
               p_391494_.apply(p_389981_),
               p_180041_ -> true,
               CloneCommands.Mode.NORMAL,
               p_396137_
            )
         )
         .then(wrapWithCloneMode(p_391282_, p_397375_, p_391494_, p_264738_ -> p_180033_ -> true, p_396137_, Commands.literal("replace")))
         .then(wrapWithCloneMode(p_391282_, p_397375_, p_391494_, p_264744_ -> FILTER_AIR, p_396137_, Commands.literal("masked")))
         .then(
            Commands.literal("filtered")
               .then(
                  wrapWithCloneMode(
                     p_391282_,
                     p_397375_,
                     p_391494_,
                     p_264745_ -> BlockPredicateArgument.getBlockPredicate(p_264745_, "filter"),
                     p_396137_,
                     Commands.argument("filter", BlockPredicateArgument.blockPredicate(p_391431_))
                  )
               )
         );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> p_394293_,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> p_396869_,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> p_392122_,
      InCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> p_392995_,
      boolean p_392263_,
      ArgumentBuilder<CommandSourceStack, ?> p_265069_
   ) {
      return p_265069_.executes(
            p_389997_ -> clone(
               (CommandSourceStack)p_389997_.getSource(),
               p_394293_.apply(p_389997_),
               p_396869_.apply(p_389997_),
               p_392122_.apply(p_389997_),
               p_392995_.apply(p_389997_),
               CloneCommands.Mode.NORMAL,
               p_392263_
            )
         )
         .then(
            Commands.literal("force")
               .executes(
                  p_389976_ -> clone(
                     (CommandSourceStack)p_389976_.getSource(),
                     p_394293_.apply(p_389976_),
                     p_396869_.apply(p_389976_),
                     p_392122_.apply(p_389976_),
                     p_392995_.apply(p_389976_),
                     CloneCommands.Mode.FORCE,
                     p_392263_
                  )
               )
         )
         .then(
            Commands.literal("move")
               .executes(
                  p_389987_ -> clone(
                     (CommandSourceStack)p_389987_.getSource(),
                     p_394293_.apply(p_389987_),
                     p_396869_.apply(p_389987_),
                     p_392122_.apply(p_389987_),
                     p_392995_.apply(p_389987_),
                     CloneCommands.Mode.MOVE,
                     p_392263_
                  )
               )
         )
         .then(
            Commands.literal("normal")
               .executes(
                  p_389968_ -> clone(
                     (CommandSourceStack)p_389968_.getSource(),
                     p_394293_.apply(p_389968_),
                     p_396869_.apply(p_389968_),
                     p_392122_.apply(p_389968_),
                     p_392995_.apply(p_389968_),
                     CloneCommands.Mode.NORMAL,
                     p_392263_
                  )
               )
         );
   }

   private static int clone(
      CommandSourceStack p_265047_,
      CloneCommands.DimensionAndPosition p_265232_,
      CloneCommands.DimensionAndPosition p_265188_,
      CloneCommands.DimensionAndPosition p_265594_,
      Predicate<BlockInWorld> p_265585_,
      CloneCommands.Mode p_265530_,
      boolean p_394242_
   ) throws CommandSyntaxException {
      BlockPos blockpos = p_265232_.position();
      BlockPos blockpos1 = p_265188_.position();
      BoundingBox boundingbox = BoundingBox.fromCorners(blockpos, blockpos1);
      BlockPos blockpos2 = p_265594_.position();
      BlockPos blockpos3 = blockpos2.offset(boundingbox.getLength());
      BoundingBox boundingbox1 = BoundingBox.fromCorners(blockpos2, blockpos3);
      ServerLevel serverlevel = p_265232_.dimension();
      ServerLevel serverlevel1 = p_265594_.dimension();
      if (!p_265530_.canOverlap() && serverlevel == serverlevel1 && boundingbox1.intersects(boundingbox)) {
         throw ERROR_OVERLAP.create();
      }

      int i = boundingbox.getXSpan() * boundingbox.getYSpan() * boundingbox.getZSpan();
      int j = p_265047_.getLevel().getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
      if (i > j) {
         throw ERROR_AREA_TOO_LARGE.create(j, i);
      }

      if (!serverlevel.hasChunksAt(blockpos, blockpos1) || !serverlevel1.hasChunksAt(blockpos2, blockpos3)) {
         throw BlockPosArgument.ERROR_NOT_LOADED.create();
      }

      if (serverlevel1.isDebug()) {
         throw ERROR_FAILED.create();
      }

      List<CloneCommands.CloneBlockInfo> list = Lists.newArrayList();
      List<CloneCommands.CloneBlockInfo> list1 = Lists.newArrayList();
      List<CloneCommands.CloneBlockInfo> list2 = Lists.newArrayList();
      Deque<BlockPos> deque = Lists.newLinkedList();
      int k = 0;
      ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER);

      try {
         BlockPos blockpos4 = new BlockPos(
            boundingbox1.minX() - boundingbox.minX(), boundingbox1.minY() - boundingbox.minY(), boundingbox1.minZ() - boundingbox.minZ()
         );

         for (int l = boundingbox.minZ(); l <= boundingbox.maxZ(); l++) {
            for (int i1 = boundingbox.minY(); i1 <= boundingbox.maxY(); i1++) {
               for (int j1 = boundingbox.minX(); j1 <= boundingbox.maxX(); j1++) {
                  BlockPos blockpos5 = new BlockPos(j1, i1, l);
                  BlockPos blockpos6 = blockpos5.offset(blockpos4);
                  BlockInWorld blockinworld = new BlockInWorld(serverlevel, blockpos5, false);
                  BlockState blockstate = blockinworld.getState();
                  if (p_265585_.test(blockinworld)) {
                     BlockEntity blockentity = serverlevel.getBlockEntity(blockpos5);
                     if (blockentity != null) {
                        TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(
                           problemreporter$scopedcollector.forChild(blockentity.problemPath()), p_265047_.registryAccess()
                        );
                        blockentity.saveCustomOnly(tagvalueoutput);
                        CloneCommands.CloneBlockEntityInfo clonecommands$cloneblockentityinfo = new CloneCommands.CloneBlockEntityInfo(
                           tagvalueoutput.buildResult(), blockentity.components()
                        );
                        list1.add(
                           new CloneCommands.CloneBlockInfo(blockpos6, blockstate, clonecommands$cloneblockentityinfo, serverlevel1.getBlockState(blockpos6))
                        );
                        deque.addLast(blockpos5);
                     } else if (!blockstate.isSolidRender() && !blockstate.isCollisionShapeFullBlock(serverlevel, blockpos5)) {
                        list2.add(new CloneCommands.CloneBlockInfo(blockpos6, blockstate, null, serverlevel1.getBlockState(blockpos6)));
                        deque.addFirst(blockpos5);
                     } else {
                        list.add(new CloneCommands.CloneBlockInfo(blockpos6, blockstate, null, serverlevel1.getBlockState(blockpos6)));
                        deque.addLast(blockpos5);
                     }
                  }
               }
            }
         }

         int l1 = 2 | (p_394242_ ? 816 : 0);
         if (p_265530_ == CloneCommands.Mode.MOVE) {
            for (BlockPos blockpos7 : deque) {
               serverlevel.setBlock(blockpos7, Blocks.BARRIER.defaultBlockState(), l1 | 816);
            }

            int i2 = p_394242_ ? l1 : 3;

            for (BlockPos blockpos8 : deque) {
               serverlevel.setBlock(blockpos8, Blocks.AIR.defaultBlockState(), i2);
            }
         }

         List<CloneCommands.CloneBlockInfo> list3 = Lists.newArrayList();
         list3.addAll(list);
         list3.addAll(list1);
         list3.addAll(list2);
         List<CloneCommands.CloneBlockInfo> list4 = Lists.reverse(list3);

         for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo : list4) {
            serverlevel1.setBlock(clonecommands$cloneblockinfo.pos, Blocks.BARRIER.defaultBlockState(), l1 | 816);
         }

         for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo1 : list3) {
            if (serverlevel1.setBlock(clonecommands$cloneblockinfo1.pos, clonecommands$cloneblockinfo1.state, l1)) {
               k++;
            }
         }

         for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo2 : list1) {
            BlockEntity blockentity1 = serverlevel1.getBlockEntity(clonecommands$cloneblockinfo2.pos);
            if (clonecommands$cloneblockinfo2.blockEntityInfo != null && blockentity1 != null) {
               blockentity1.loadCustomOnly(
                  TagValueInput.create(
                     problemreporter$scopedcollector.forChild(blockentity1.problemPath()),
                     serverlevel1.registryAccess(),
                     clonecommands$cloneblockinfo2.blockEntityInfo.tag
                  )
               );
               blockentity1.setComponents(clonecommands$cloneblockinfo2.blockEntityInfo.components);
               blockentity1.setChanged();
            }

            serverlevel1.setBlock(clonecommands$cloneblockinfo2.pos, clonecommands$cloneblockinfo2.state, l1);
         }

         if (!p_394242_) {
            for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo3 : list4) {
               serverlevel1.updateNeighboursOnBlockSet(clonecommands$cloneblockinfo3.pos, clonecommands$cloneblockinfo3.previousStateAtDestination);
            }
         }

         serverlevel1.getBlockTicks().copyAreaFrom(serverlevel.getBlockTicks(), boundingbox, blockpos4);
      } catch (Throwable throwable1) {
         try {
            problemreporter$scopedcollector.close();
         } catch (Throwable throwable) {
            throwable1.addSuppressed(throwable);
         }

         throw throwable1;
      }

      problemreporter$scopedcollector.close();
      if (k == 0) {
         throw ERROR_FAILED.create();
      }

      int k1 = k;
      p_265047_.sendSuccess(() -> Component.translatable("commands.clone.success", k1), true);
      return k;
   }

   record CloneBlockEntityInfo(CompoundTag tag, DataComponentMap components) {
   }

   record CloneBlockInfo(BlockPos pos, BlockState state, CloneCommands.@Nullable CloneBlockEntityInfo blockEntityInfo, BlockState previousStateAtDestination) {
   }

   record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
   }

   enum Mode {
      FORCE(true),
      MOVE(true),
      NORMAL(false);

      private final boolean canOverlap;

      Mode(final boolean p_136795_) {
         this.canOverlap = p_136795_;
      }

      public boolean canOverlap() {
         return this.canOverlap;
      }
   }
}
