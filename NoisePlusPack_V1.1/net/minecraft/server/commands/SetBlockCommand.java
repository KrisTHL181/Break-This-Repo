package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jspecify.annotations.Nullable;

public class SetBlockCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.setblock.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_214731_, CommandBuildContext p_214732_) {
      Predicate<BlockInWorld> predicate = p_180517_ -> p_180517_.getLevel().isEmptyBlock(p_180517_.getPos());
      p_214731_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setblock").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("pos", BlockPosArgument.blockPos())
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                       "block", BlockStateArgument.block(p_214732_)
                                    )
                                    .executes(
                                       p_390090_ -> setBlock(
                                          (CommandSourceStack)p_390090_.getSource(),
                                          BlockPosArgument.getLoadedBlockPos(p_390090_, "pos"),
                                          BlockStateArgument.getBlock(p_390090_, "block"),
                                          SetBlockCommand.Mode.REPLACE,
                                          null,
                                          false
                                       )
                                    ))
                                 .then(
                                    Commands.literal("destroy")
                                       .executes(
                                          p_390093_ -> setBlock(
                                             (CommandSourceStack)p_390093_.getSource(),
                                             BlockPosArgument.getLoadedBlockPos(p_390093_, "pos"),
                                             BlockStateArgument.getBlock(p_390093_, "block"),
                                             SetBlockCommand.Mode.DESTROY,
                                             null,
                                             false
                                          )
                                       )
                                 ))
                              .then(
                                 Commands.literal("keep")
                                    .executes(
                                       p_390095_ -> setBlock(
                                          (CommandSourceStack)p_390095_.getSource(),
                                          BlockPosArgument.getLoadedBlockPos(p_390095_, "pos"),
                                          BlockStateArgument.getBlock(p_390095_, "block"),
                                          SetBlockCommand.Mode.REPLACE,
                                          predicate,
                                          false
                                       )
                                    )
                              ))
                           .then(
                              Commands.literal("replace")
                                 .executes(
                                    p_390092_ -> setBlock(
                                       (CommandSourceStack)p_390092_.getSource(),
                                       BlockPosArgument.getLoadedBlockPos(p_390092_, "pos"),
                                       BlockStateArgument.getBlock(p_390092_, "block"),
                                       SetBlockCommand.Mode.REPLACE,
                                       null,
                                       false
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("strict")
                              .executes(
                                 p_390091_ -> setBlock(
                                    (CommandSourceStack)p_390091_.getSource(),
                                    BlockPosArgument.getLoadedBlockPos(p_390091_, "pos"),
                                    BlockStateArgument.getBlock(p_390091_, "block"),
                                    SetBlockCommand.Mode.REPLACE,
                                    null,
                                    true
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int setBlock(
      CommandSourceStack p_138608_,
      BlockPos p_138609_,
      BlockInput p_138610_,
      SetBlockCommand.Mode p_138611_,
      @Nullable Predicate<BlockInWorld> p_138612_,
      boolean p_391792_
   ) throws CommandSyntaxException {
      ServerLevel serverlevel = p_138608_.getLevel();
      if (serverlevel.isDebug()) {
         throw ERROR_FAILED.create();
      }

      if (p_138612_ != null && !p_138612_.test(new BlockInWorld(serverlevel, p_138609_, true))) {
         throw ERROR_FAILED.create();
      }

      boolean flag;
      if (p_138611_ == SetBlockCommand.Mode.DESTROY) {
         serverlevel.destroyBlock(p_138609_, true);
         flag = !p_138610_.getState().isAir() || !serverlevel.getBlockState(p_138609_).isAir();
      } else {
         flag = true;
      }

      BlockState blockstate = serverlevel.getBlockState(p_138609_);
      if (flag && !p_138610_.place(serverlevel, p_138609_, 2 | (p_391792_ ? 816 : 256))) {
         throw ERROR_FAILED.create();
      }

      if (!p_391792_) {
         serverlevel.updateNeighboursOnBlockSet(p_138609_, blockstate);
      }

      p_138608_.sendSuccess(() -> Component.translatable("commands.setblock.success", p_138609_.getX(), p_138609_.getY(), p_138609_.getZ()), true);
      return 1;
   }

   public enum Mode {
      REPLACE,
      DESTROY;
   }
}
