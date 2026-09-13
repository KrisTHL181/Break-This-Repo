package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

public class FillCommand {
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (p_308702_, p_308703_) -> Component.translatableEscape("commands.fill.toobig", p_308702_, p_308703_)
   );
   static final BlockInput HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), null);
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.fill.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_214443_, CommandBuildContext p_214444_) {
      p_214443_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fill").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("to", BlockPosArgument.blockPos())
                        .then(
                           wrapWithMode(
                                 p_214444_,
                                 Commands.argument("block", BlockStateArgument.block(p_214444_)),
                                 p_390046_ -> BlockPosArgument.getLoadedBlockPos(p_390046_, "from"),
                                 p_390024_ -> BlockPosArgument.getLoadedBlockPos(p_390024_, "to"),
                                 p_390018_ -> BlockStateArgument.getBlock(p_390018_, "block"),
                                 p_390033_ -> null
                              )
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("replace")
                                       .executes(
                                          p_390025_ -> fillBlocks(
                                             (CommandSourceStack)p_390025_.getSource(),
                                             BoundingBox.fromCorners(
                                                BlockPosArgument.getLoadedBlockPos(p_390025_, "from"), BlockPosArgument.getLoadedBlockPos(p_390025_, "to")
                                             ),
                                             BlockStateArgument.getBlock(p_390025_, "block"),
                                             FillCommand.Mode.REPLACE,
                                             null,
                                             false
                                          )
                                       ))
                                    .then(
                                       wrapWithMode(
                                          p_214444_,
                                          Commands.argument("filter", BlockPredicateArgument.blockPredicate(p_214444_)),
                                          p_390027_ -> BlockPosArgument.getLoadedBlockPos(p_390027_, "from"),
                                          p_390040_ -> BlockPosArgument.getLoadedBlockPos(p_390040_, "to"),
                                          p_390047_ -> BlockStateArgument.getBlock(p_390047_, "block"),
                                          p_390034_ -> BlockPredicateArgument.getBlockPredicate(p_390034_, "filter")
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("keep")
                                    .executes(
                                       p_390026_ -> fillBlocks(
                                          (CommandSourceStack)p_390026_.getSource(),
                                          BoundingBox.fromCorners(
                                             BlockPosArgument.getLoadedBlockPos(p_390026_, "from"), BlockPosArgument.getLoadedBlockPos(p_390026_, "to")
                                          ),
                                          BlockStateArgument.getBlock(p_390026_, "block"),
                                          FillCommand.Mode.REPLACE,
                                          p_180225_ -> p_180225_.getLevel().isEmptyBlock(p_180225_.getPos()),
                                          false
                                       )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithMode(
      CommandBuildContext p_397191_,
      ArgumentBuilder<CommandSourceStack, ?> p_391762_,
      InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> p_397447_,
      InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> p_394894_,
      InCommandFunction<CommandContext<CommandSourceStack>, BlockInput> p_397837_,
      FillCommand.NullableCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> p_397183_
   ) {
      return p_391762_.executes(
            p_390039_ -> fillBlocks(
               (CommandSourceStack)p_390039_.getSource(),
               BoundingBox.fromCorners(p_397447_.apply(p_390039_), p_394894_.apply(p_390039_)),
               p_397837_.apply(p_390039_),
               FillCommand.Mode.REPLACE,
               p_397183_.apply(p_390039_),
               false
            )
         )
         .then(
            Commands.literal("outline")
               .executes(
                  p_390032_ -> fillBlocks(
                     (CommandSourceStack)p_390032_.getSource(),
                     BoundingBox.fromCorners(p_397447_.apply(p_390032_), p_394894_.apply(p_390032_)),
                     p_397837_.apply(p_390032_),
                     FillCommand.Mode.OUTLINE,
                     p_397183_.apply(p_390032_),
                     false
                  )
               )
         )
         .then(
            Commands.literal("hollow")
               .executes(
                  p_390023_ -> fillBlocks(
                     (CommandSourceStack)p_390023_.getSource(),
                     BoundingBox.fromCorners(p_397447_.apply(p_390023_), p_394894_.apply(p_390023_)),
                     p_397837_.apply(p_390023_),
                     FillCommand.Mode.HOLLOW,
                     p_397183_.apply(p_390023_),
                     false
                  )
               )
         )
         .then(
            Commands.literal("destroy")
               .executes(
                  p_390045_ -> fillBlocks(
                     (CommandSourceStack)p_390045_.getSource(),
                     BoundingBox.fromCorners(p_397447_.apply(p_390045_), p_394894_.apply(p_390045_)),
                     p_397837_.apply(p_390045_),
                     FillCommand.Mode.DESTROY,
                     p_397183_.apply(p_390045_),
                     false
                  )
               )
         )
         .then(
            Commands.literal("strict")
               .executes(
                  p_390017_ -> fillBlocks(
                     (CommandSourceStack)p_390017_.getSource(),
                     BoundingBox.fromCorners(p_397447_.apply(p_390017_), p_394894_.apply(p_390017_)),
                     p_397837_.apply(p_390017_),
                     FillCommand.Mode.REPLACE,
                     p_397183_.apply(p_390017_),
                     true
                  )
               )
         );
   }

   private static int fillBlocks(
      CommandSourceStack p_137386_,
      BoundingBox p_137387_,
      BlockInput p_137388_,
      FillCommand.Mode p_137389_,
      @Nullable Predicate<BlockInWorld> p_137390_,
      boolean p_395183_
   ) throws CommandSyntaxException {
      int i = p_137387_.getXSpan() * p_137387_.getYSpan() * p_137387_.getZSpan();
      int j = p_137386_.getLevel().getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
      if (i > j) {
         throw ERROR_AREA_TOO_LARGE.create(j, i);
      }

      record UpdatedPosition(BlockPos pos, BlockState oldState) {
      }

      List<UpdatedPosition> list = Lists.newArrayList();
      ServerLevel serverlevel = p_137386_.getLevel();
      if (serverlevel.isDebug()) {
         throw ERROR_FAILED.create();
      }

      int k = 0;

      for (BlockPos blockpos : BlockPos.betweenClosed(
         p_137387_.minX(), p_137387_.minY(), p_137387_.minZ(), p_137387_.maxX(), p_137387_.maxY(), p_137387_.maxZ()
      )) {
         if (p_137390_ == null || p_137390_.test(new BlockInWorld(serverlevel, blockpos, true))) {
            BlockState blockstate = serverlevel.getBlockState(blockpos);
            boolean flag = false;
            if (p_137389_.affector.affect(serverlevel, blockpos)) {
               flag = true;
            }

            BlockInput blockinput = p_137389_.filter.filter(p_137387_, blockpos, p_137388_, serverlevel);
            if (blockinput == null) {
               if (flag) {
                  k++;
               }
            } else if (!blockinput.place(serverlevel, blockpos, 2 | (p_395183_ ? 816 : 256))) {
               if (flag) {
                  k++;
               }
            } else {
               if (!p_395183_) {
                  list.add(new UpdatedPosition(blockpos.immutable(), blockstate));
               }

               k++;
            }
         }
      }

      for (UpdatedPosition fillcommand$1updatedposition : list) {
         serverlevel.updateNeighboursOnBlockSet(fillcommand$1updatedposition.pos, fillcommand$1updatedposition.oldState);
      }

      if (k == 0) {
         throw ERROR_FAILED.create();
      }

      int l = k;
      p_137386_.sendSuccess(() -> Component.translatable("commands.fill.success", l), true);
      return k;
   }

   @FunctionalInterface
   public interface Affector {
      FillCommand.Affector NOOP = (p_397097_, p_396648_) -> false;

      boolean affect(ServerLevel var1, BlockPos var2);
   }

   @FunctionalInterface
   public interface Filter {
      FillCommand.Filter NOOP = (p_393263_, p_397927_, p_393586_, p_393914_) -> p_393586_;

      @Nullable BlockInput filter(BoundingBox var1, BlockPos var2, BlockInput var3, ServerLevel var4);
   }

   enum Mode {
      REPLACE(FillCommand.Affector.NOOP, FillCommand.Filter.NOOP),
      OUTLINE(
         FillCommand.Affector.NOOP,
         (p_137428_, p_137429_, p_137430_, p_137431_) -> p_137429_.getX() != p_137428_.minX()
               && p_137429_.getX() != p_137428_.maxX()
               && p_137429_.getY() != p_137428_.minY()
               && p_137429_.getY() != p_137428_.maxY()
               && p_137429_.getZ() != p_137428_.minZ()
               && p_137429_.getZ() != p_137428_.maxZ()
            ? null
            : p_137430_
      ),
      HOLLOW(
         FillCommand.Affector.NOOP,
         (p_137423_, p_137424_, p_137425_, p_137426_) -> p_137424_.getX() != p_137423_.minX()
               && p_137424_.getX() != p_137423_.maxX()
               && p_137424_.getY() != p_137423_.minY()
               && p_137424_.getY() != p_137423_.maxY()
               && p_137424_.getZ() != p_137423_.minZ()
               && p_137424_.getZ() != p_137423_.maxZ()
            ? FillCommand.HOLLOW_CORE
            : p_137425_
      ),
      DESTROY((p_390048_, p_390049_) -> p_390048_.destroyBlock(p_390049_, true), FillCommand.Filter.NOOP);

      public final FillCommand.Filter filter;
      public final FillCommand.Affector affector;

      Mode(final FillCommand.Affector p_395104_, final FillCommand.Filter p_392997_) {
         this.affector = p_395104_;
         this.filter = p_392997_;
      }
   }

   @FunctionalInterface
   interface NullableCommandFunction<T, R> {
      @Nullable R apply(T var1) throws CommandSyntaxException;
   }
}
