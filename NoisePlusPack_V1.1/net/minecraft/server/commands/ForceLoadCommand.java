package net.minecraft.server.commands;

import com.google.common.base.Joiner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ForceLoadCommand {
   private static final int MAX_CHUNK_LIMIT = 256;
   private static final Dynamic2CommandExceptionType ERROR_TOO_MANY_CHUNKS = new Dynamic2CommandExceptionType(
      (p_308708_, p_308709_) -> Component.translatableEscape("commands.forceload.toobig", p_308708_, p_308709_)
   );
   private static final Dynamic2CommandExceptionType ERROR_NOT_TICKING = new Dynamic2CommandExceptionType(
      (p_308720_, p_308721_) -> Component.translatableEscape("commands.forceload.query.failure", p_308720_, p_308721_)
   );
   private static final SimpleCommandExceptionType ERROR_ALL_ADDED = new SimpleCommandExceptionType(Component.translatable("commands.forceload.added.failure"));
   private static final SimpleCommandExceptionType ERROR_NONE_REMOVED = new SimpleCommandExceptionType(
      Component.translatable("commands.forceload.removed.failure")
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_137677_) {
      p_137677_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("forceload")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     Commands.literal("add")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("from", ColumnPosArgument.columnPos())
                                 .executes(
                                    p_137711_ -> changeForceLoad(
                                       (CommandSourceStack)p_137711_.getSource(),
                                       ColumnPosArgument.getColumnPos(p_137711_, "from"),
                                       ColumnPosArgument.getColumnPos(p_137711_, "from"),
                                       true
                                    )
                                 ))
                              .then(
                                 Commands.argument("to", ColumnPosArgument.columnPos())
                                    .executes(
                                       p_137709_ -> changeForceLoad(
                                          (CommandSourceStack)p_137709_.getSource(),
                                          ColumnPosArgument.getColumnPos(p_137709_, "from"),
                                          ColumnPosArgument.getColumnPos(p_137709_, "to"),
                                          true
                                       )
                                    )
                              )
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("remove")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("from", ColumnPosArgument.columnPos())
                                 .executes(
                                    p_137707_ -> changeForceLoad(
                                       (CommandSourceStack)p_137707_.getSource(),
                                       ColumnPosArgument.getColumnPos(p_137707_, "from"),
                                       ColumnPosArgument.getColumnPos(p_137707_, "from"),
                                       false
                                    )
                                 ))
                              .then(
                                 Commands.argument("to", ColumnPosArgument.columnPos())
                                    .executes(
                                       p_137705_ -> changeForceLoad(
                                          (CommandSourceStack)p_137705_.getSource(),
                                          ColumnPosArgument.getColumnPos(p_137705_, "from"),
                                          ColumnPosArgument.getColumnPos(p_137705_, "to"),
                                          false
                                       )
                                    )
                              )
                        ))
                     .then(Commands.literal("all").executes(p_137701_ -> removeAll((CommandSourceStack)p_137701_.getSource())))
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("query").executes(p_137694_ -> listForceLoad((CommandSourceStack)p_137694_.getSource())))
                  .then(
                     Commands.argument("pos", ColumnPosArgument.columnPos())
                        .executes(p_137679_ -> queryForceLoad((CommandSourceStack)p_137679_.getSource(), ColumnPosArgument.getColumnPos(p_137679_, "pos")))
                  )
            )
      );
   }

   private static int queryForceLoad(CommandSourceStack p_137683_, ColumnPos p_137684_) throws CommandSyntaxException {
      ChunkPos chunkpos = p_137684_.toChunkPos();
      ServerLevel serverlevel = p_137683_.getLevel();
      ResourceKey<Level> resourcekey = serverlevel.dimension();
      boolean flag = serverlevel.getForceLoadedChunks().contains(chunkpos.toLong());
      if (flag) {
         p_137683_.sendSuccess(
            () -> Component.translatable(
               "commands.forceload.query.success", Component.translationArg(chunkpos), Component.translationArg(resourcekey.identifier())
            ),
            false
         );
         return 1;
      } else {
         throw ERROR_NOT_TICKING.create(chunkpos, resourcekey.identifier());
      }
   }

   private static int listForceLoad(CommandSourceStack p_137681_) {
      ServerLevel serverlevel = p_137681_.getLevel();
      ResourceKey<Level> resourcekey = serverlevel.dimension();
      LongSet longset = serverlevel.getForceLoadedChunks();
      int i = longset.size();
      if (i > 0) {
         String s = Joiner.on(", ").join(longset.stream().sorted().map(ChunkPos::new).map(ChunkPos::toString).iterator());
         if (i == 1) {
            p_137681_.sendSuccess(() -> Component.translatable("commands.forceload.list.single", Component.translationArg(resourcekey.identifier()), s), false);
         } else {
            p_137681_.sendSuccess(
               () -> Component.translatable("commands.forceload.list.multiple", i, Component.translationArg(resourcekey.identifier()), s), false
            );
         }
      } else {
         p_137681_.sendFailure(Component.translatable("commands.forceload.added.none", Component.translationArg(resourcekey.identifier())));
      }

      return i;
   }

   private static int removeAll(CommandSourceStack p_137696_) {
      ServerLevel serverlevel = p_137696_.getLevel();
      ResourceKey<Level> resourcekey = serverlevel.dimension();
      LongSet longset = serverlevel.getForceLoadedChunks();
      longset.forEach(p_137675_ -> serverlevel.setChunkForced(ChunkPos.getX(p_137675_), ChunkPos.getZ(p_137675_), false));
      p_137696_.sendSuccess(() -> Component.translatable("commands.forceload.removed.all", Component.translationArg(resourcekey.identifier())), true);
      return 0;
   }

   private static int changeForceLoad(CommandSourceStack p_137686_, ColumnPos p_137687_, ColumnPos p_137688_, boolean p_137689_) throws CommandSyntaxException {
      int i = Math.min(p_137687_.x(), p_137688_.x());
      int j = Math.min(p_137687_.z(), p_137688_.z());
      int k = Math.max(p_137687_.x(), p_137688_.x());
      int l = Math.max(p_137687_.z(), p_137688_.z());
      int i1 = SectionPos.blockToSectionCoord(i);
      int j1 = SectionPos.blockToSectionCoord(j);
      int k1 = SectionPos.blockToSectionCoord(k);
      int l1 = SectionPos.blockToSectionCoord(l);
      long i2 = (k1 - i1 + 1L) * (l1 - j1 + 1L);
      if (i2 > 256L) {
         throw ERROR_TOO_MANY_CHUNKS.create(256, i2);
      }

      ServerLevel serverlevel = p_137686_.getLevel();
      ResourceKey<Level> resourcekey = serverlevel.dimension();
      ChunkPos chunkpos = null;
      int j2 = 0;

      for (int k2 = i1; k2 <= k1; k2++) {
         for (int l2 = j1; l2 <= l1; l2++) {
            boolean flag = serverlevel.setChunkForced(k2, l2, p_137689_);
            if (flag) {
               j2++;
               if (chunkpos == null) {
                  chunkpos = new ChunkPos(k2, l2);
               }
            }
         }
      }

      ChunkPos chunkpos2 = chunkpos;
      int i3 = j2;
      if (i3 == 0) {
         throw (p_137689_ ? ERROR_ALL_ADDED : ERROR_NONE_REMOVED).create();
      }

      if (i3 == 1) {
         p_137686_.sendSuccess(
            () -> Component.translatable(
               "commands.forceload." + (p_137689_ ? "added" : "removed") + ".single",
               Component.translationArg(chunkpos2),
               Component.translationArg(resourcekey.identifier())
            ),
            true
         );
      } else {
         ChunkPos chunkpos3 = new ChunkPos(i1, j1);
         ChunkPos chunkpos1 = new ChunkPos(k1, l1);
         p_137686_.sendSuccess(
            () -> Component.translatable(
               "commands.forceload." + (p_137689_ ? "added" : "removed") + ".multiple",
               i3,
               Component.translationArg(resourcekey.identifier()),
               Component.translationArg(chunkpos3),
               Component.translationArg(chunkpos1)
            ),
            true
         );
      }

      return i3;
   }
}
