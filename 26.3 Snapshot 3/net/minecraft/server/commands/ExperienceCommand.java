package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ExperienceCommand {
   private static final SimpleCommandExceptionType ERROR_SET_POINTS_INVALID = new SimpleCommandExceptionType(
      Component.translatable("commands.experience.set.points.invalid")
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralCommandNode<CommandSourceStack> command = dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("experience")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     Commands.literal("add")
                        .then(
                           Commands.argument("target", EntityArgument.players())
                              .then(
                                 ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("amount", IntegerArgumentType.integer())
                                          .executes(
                                             c -> addExperience(
                                                (CommandSourceStack)c.getSource(),
                                                EntityArgument.getPlayers(c, "target"),
                                                IntegerArgumentType.getInteger(c, "amount"),
                                                ExperienceCommand.Type.POINTS
                                             )
                                          ))
                                       .then(
                                          Commands.literal("points")
                                             .executes(
                                                c -> addExperience(
                                                   (CommandSourceStack)c.getSource(),
                                                   EntityArgument.getPlayers(c, "target"),
                                                   IntegerArgumentType.getInteger(c, "amount"),
                                                   ExperienceCommand.Type.POINTS
                                                )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("levels")
                                          .executes(
                                             c -> addExperience(
                                                (CommandSourceStack)c.getSource(),
                                                EntityArgument.getPlayers(c, "target"),
                                                IntegerArgumentType.getInteger(c, "amount"),
                                                ExperienceCommand.Type.LEVELS
                                             )
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("set")
                     .then(
                        Commands.argument("target", EntityArgument.players())
                           .then(
                              ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("amount", IntegerArgumentType.integer(0))
                                       .executes(
                                          c -> setExperience(
                                             (CommandSourceStack)c.getSource(),
                                             EntityArgument.getPlayers(c, "target"),
                                             IntegerArgumentType.getInteger(c, "amount"),
                                             ExperienceCommand.Type.POINTS
                                          )
                                       ))
                                    .then(
                                       Commands.literal("points")
                                          .executes(
                                             c -> setExperience(
                                                (CommandSourceStack)c.getSource(),
                                                EntityArgument.getPlayers(c, "target"),
                                                IntegerArgumentType.getInteger(c, "amount"),
                                                ExperienceCommand.Type.POINTS
                                             )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("levels")
                                       .executes(
                                          c -> setExperience(
                                             (CommandSourceStack)c.getSource(),
                                             EntityArgument.getPlayers(c, "target"),
                                             IntegerArgumentType.getInteger(c, "amount"),
                                             ExperienceCommand.Type.LEVELS
                                          )
                                       )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("query")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.player())
                           .then(
                              Commands.literal("points")
                                 .executes(
                                    c -> queryExperience(
                                       (CommandSourceStack)c.getSource(), EntityArgument.getPlayer(c, "target"), ExperienceCommand.Type.POINTS
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("levels")
                              .executes(
                                 c -> queryExperience((CommandSourceStack)c.getSource(), EntityArgument.getPlayer(c, "target"), ExperienceCommand.Type.LEVELS)
                              )
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("xp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .redirect(command)
      );
   }

   private static int queryExperience(final CommandSourceStack source, final ServerPlayer target, final ExperienceCommand.Type type) {
      int result = type.query.applyAsInt(target);
      source.sendSuccess(() -> type.queryResponse(target, result), false);
      return result;
   }

   private static int addExperience(
      final CommandSourceStack source, final Collection<? extends ServerPlayer> players, final int amount, final ExperienceCommand.Type type
   ) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : players) {
         type.add.accept(player, amount);
         tracker.track(player);
      }

      return tracker.sendFeedback(source, true, type.addResponse, amount);
   }

   private static int setExperience(
      final CommandSourceStack source, final Collection<? extends ServerPlayer> players, final int amount, final ExperienceCommand.Type type
   ) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : players) {
         tracker.track(player, type.set.test(player, amount));
      }

      return tracker.sendFeedback(source, true, type.setResponse, amount);
   }

   private enum Type {
      POINTS("points", Player::giveExperiencePoints, (p, a) -> {
         if (a >= p.getXpNeededForNextLevel()) {
            return false;
         }

         p.setExperiencePoints(a);
         return true;
      }, p -> Mth.floor(p.experienceProgress * p.getXpNeededForNextLevel())),
      LEVELS("levels", ServerPlayer::giveExperienceLevels, (p, a) -> {
         p.setExperienceLevels(a);
         return true;
      }, p -> p.experienceLevel);

      public final BiConsumer<ServerPlayer, Integer> add;
      public final BiPredicate<ServerPlayer, Integer> set;
      public final ToIntFunction<ServerPlayer> query;
      public final String queryTranslationKey;
      public final CommandResponseTracker.MessagesWithArg<ServerPlayer, Integer> addResponse;
      public final CommandResponseTracker.MessagesWithArg<ServerPlayer, Integer> setResponse;

      Type(
         final String name, final BiConsumer<ServerPlayer, Integer> add, final BiPredicate<ServerPlayer, Integer> set, final ToIntFunction<ServerPlayer> query
      ) {
         this.add = add;
         this.addResponse = CommandResponseTracker.messages(
            (player, var2, amount) -> Component.translatable("commands.experience.add." + name + ".success.single", amount, player.getDisplayName()),
            (playerCount, var2, amount) -> Component.translatable("commands.experience.add." + name + ".success.multiple", amount, playerCount)
         );
         this.set = set;
         this.setResponse = CommandResponseTracker.messages(
            ExperienceCommand.ERROR_SET_POINTS_INVALID,
            (player, var2, amount) -> Component.translatable("commands.experience.set." + name + ".success.single", amount, player.getDisplayName()),
            (playerCount, var2, amount) -> Component.translatable("commands.experience.set." + name + ".success.multiple", amount, playerCount)
         );
         this.query = query;
         this.queryTranslationKey = "commands.experience.query." + name;
      }

      public Component queryResponse(final ServerPlayer target, final int result) {
         return Component.translatable(this.queryTranslationKey, target.getDisplayName(), result);
      }
   }
}
