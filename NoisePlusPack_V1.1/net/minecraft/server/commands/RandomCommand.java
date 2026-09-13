package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequences;
import org.jspecify.annotations.Nullable;

public class RandomCommand {
   private static final SimpleCommandExceptionType ERROR_RANGE_TOO_LARGE = new SimpleCommandExceptionType(
      Component.translatable("commands.random.error.range_too_large")
   );
   private static final SimpleCommandExceptionType ERROR_RANGE_TOO_SMALL = new SimpleCommandExceptionType(
      Component.translatable("commands.random.error.range_too_small")
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_300897_) {
      p_300897_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("random").then(drawRandomValueTree("value", false)))
               .then(drawRandomValueTree("roll", true)))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("*").executes(p_300657_ -> resetAllSequences((CommandSourceStack)p_300657_.getSource())))
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer())
                                    .executes(
                                       p_300850_ -> resetAllSequencesAndSetNewDefaults(
                                          (CommandSourceStack)p_300850_.getSource(), IntegerArgumentType.getInteger(p_300850_, "seed"), true, true
                                       )
                                    ))
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("includeWorldSeed", BoolArgumentType.bool())
                                          .executes(
                                             p_299490_ -> resetAllSequencesAndSetNewDefaults(
                                                (CommandSourceStack)p_299490_.getSource(),
                                                IntegerArgumentType.getInteger(p_299490_, "seed"),
                                                BoolArgumentType.getBool(p_299490_, "includeWorldSeed"),
                                                true
                                             )
                                          ))
                                       .then(
                                          Commands.argument("includeSequenceId", BoolArgumentType.bool())
                                             .executes(
                                                p_299589_ -> resetAllSequencesAndSetNewDefaults(
                                                   (CommandSourceStack)p_299589_.getSource(),
                                                   IntegerArgumentType.getInteger(p_299589_, "seed"),
                                                   BoolArgumentType.getBool(p_299589_, "includeWorldSeed"),
                                                   BoolArgumentType.getBool(p_299589_, "includeSequenceId")
                                                )
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("sequence", IdentifierArgument.id())
                           .suggests(RandomCommand::suggestRandomSequence)
                           .executes(p_449013_ -> resetSequence((CommandSourceStack)p_449013_.getSource(), IdentifierArgument.getId(p_449013_, "sequence"))))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer())
                                 .executes(
                                    p_449012_ -> resetSequence(
                                       (CommandSourceStack)p_449012_.getSource(),
                                       IdentifierArgument.getId(p_449012_, "sequence"),
                                       IntegerArgumentType.getInteger(p_449012_, "seed"),
                                       true,
                                       true
                                    )
                                 ))
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("includeWorldSeed", BoolArgumentType.bool())
                                       .executes(
                                          p_449009_ -> resetSequence(
                                             (CommandSourceStack)p_449009_.getSource(),
                                             IdentifierArgument.getId(p_449009_, "sequence"),
                                             IntegerArgumentType.getInteger(p_449009_, "seed"),
                                             BoolArgumentType.getBool(p_449009_, "includeWorldSeed"),
                                             true
                                          )
                                       ))
                                    .then(
                                       Commands.argument("includeSequenceId", BoolArgumentType.bool())
                                          .executes(
                                             p_449014_ -> resetSequence(
                                                (CommandSourceStack)p_449014_.getSource(),
                                                IdentifierArgument.getId(p_449014_, "sequence"),
                                                IntegerArgumentType.getInteger(p_449014_, "seed"),
                                                BoolArgumentType.getBool(p_449014_, "includeWorldSeed"),
                                                BoolArgumentType.getBool(p_449014_, "includeSequenceId")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> drawRandomValueTree(String p_299144_, boolean p_298789_) {
      return (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal(p_299144_)
         .then(
            ((RequiredArgumentBuilder)Commands.argument("range", RangeArgument.intRange())
                  .executes(
                     p_449011_ -> randomSample((CommandSourceStack)p_449011_.getSource(), RangeArgument.Ints.getRange(p_449011_, "range"), null, p_298789_)
                  ))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("sequence", IdentifierArgument.id())
                        .suggests(RandomCommand::suggestRandomSequence)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .executes(
                        p_449016_ -> randomSample(
                           (CommandSourceStack)p_449016_.getSource(),
                           RangeArgument.Ints.getRange(p_449016_, "range"),
                           IdentifierArgument.getId(p_449016_, "sequence"),
                           p_298789_
                        )
                     )
               )
         );
   }

   private static CompletableFuture<Suggestions> suggestRandomSequence(CommandContext<CommandSourceStack> p_297521_, SuggestionsBuilder p_299165_) {
      List<String> list = Lists.newArrayList();
      ((CommandSourceStack)p_297521_.getSource()).getLevel().getRandomSequences().forAllSequences((p_449019_, p_449020_) -> list.add(p_449019_.toString()));
      return SharedSuggestionProvider.suggest(list, p_299165_);
   }

   private static int randomSample(CommandSourceStack p_299745_, MinMaxBounds.Ints p_455001_, @Nullable Identifier p_453629_, boolean p_298006_) throws CommandSyntaxException {
      RandomSource randomsource;
      if (p_453629_ != null) {
         randomsource = p_299745_.getLevel().getRandomSequence(p_453629_);
      } else {
         randomsource = p_299745_.getLevel().getRandom();
      }

      int i = p_455001_.min().orElse(Integer.MIN_VALUE);
      int j = p_455001_.max().orElse(Integer.MAX_VALUE);
      long k = (long)j - i;
      if (k == 0L) {
         throw ERROR_RANGE_TOO_SMALL.create();
      }

      if (k >= 2147483647L) {
         throw ERROR_RANGE_TOO_LARGE.create();
      }

      int l = Mth.randomBetweenInclusive(randomsource, i, j);
      if (p_298006_) {
         p_299745_.getServer()
            .getPlayerList()
            .broadcastSystemMessage(Component.translatable("commands.random.roll", p_299745_.getDisplayName(), l, i, j), false);
      } else {
         p_299745_.sendSuccess(() -> Component.translatable("commands.random.sample.success", l), false);
      }

      return l;
   }

   private static int resetSequence(CommandSourceStack p_300119_, Identifier p_456774_) throws CommandSyntaxException {
      ServerLevel serverlevel = p_300119_.getLevel();
      serverlevel.getRandomSequences().reset(p_456774_, serverlevel.getSeed());
      p_300119_.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(p_456774_)), false);
      return 1;
   }

   private static int resetSequence(CommandSourceStack p_298149_, Identifier p_453504_, int p_300974_, boolean p_298057_, boolean p_300002_) throws CommandSyntaxException {
      ServerLevel serverlevel = p_298149_.getLevel();
      serverlevel.getRandomSequences().reset(p_453504_, serverlevel.getSeed(), p_300974_, p_298057_, p_300002_);
      p_298149_.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(p_453504_)), false);
      return 1;
   }

   private static int resetAllSequences(CommandSourceStack p_299139_) {
      int i = p_299139_.getLevel().getRandomSequences().clear();
      p_299139_.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", i), false);
      return i;
   }

   private static int resetAllSequencesAndSetNewDefaults(CommandSourceStack p_299873_, int p_300494_, boolean p_300184_, boolean p_297446_) {
      RandomSequences randomsequences = p_299873_.getLevel().getRandomSequences();
      randomsequences.setSeedDefaults(p_300494_, p_300184_, p_297446_);
      int i = randomsequences.clear();
      p_299873_.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", i), false);
      return i;
   }
}
