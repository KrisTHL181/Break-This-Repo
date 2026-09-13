package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeCommand {
   private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.give.failed"));
   private static final SimpleCommandExceptionType ERROR_TAKE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.take.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_138201_) {
      p_138201_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("recipe")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("give")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                              .then(
                                 Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE))
                                    .executes(
                                       p_358619_ -> giveRecipes(
                                          (CommandSourceStack)p_358619_.getSource(),
                                          EntityArgument.getPlayers(p_358619_, "targets"),
                                          Collections.singleton(ResourceKeyArgument.getRecipe(p_358619_, "recipe"))
                                       )
                                    )
                              ))
                           .then(
                              Commands.literal("*")
                                 .executes(
                                    p_138217_ -> giveRecipes(
                                       (CommandSourceStack)p_138217_.getSource(),
                                       EntityArgument.getPlayers(p_138217_, "targets"),
                                       ((CommandSourceStack)p_138217_.getSource()).getServer().getRecipeManager().getRecipes()
                                    )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("take")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                           .then(
                              Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE))
                                 .executes(
                                    p_358618_ -> takeRecipes(
                                       (CommandSourceStack)p_358618_.getSource(),
                                       EntityArgument.getPlayers(p_358618_, "targets"),
                                       Collections.singleton(ResourceKeyArgument.getRecipe(p_358618_, "recipe"))
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("*")
                              .executes(
                                 p_138203_ -> takeRecipes(
                                    (CommandSourceStack)p_138203_.getSource(),
                                    EntityArgument.getPlayers(p_138203_, "targets"),
                                    ((CommandSourceStack)p_138203_.getSource()).getServer().getRecipeManager().getRecipes()
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int giveRecipes(CommandSourceStack p_138207_, Collection<ServerPlayer> p_138208_, Collection<RecipeHolder<?>> p_138209_) throws CommandSyntaxException {
      int i = 0;

      for (ServerPlayer serverplayer : p_138208_) {
         i += serverplayer.awardRecipes(p_138209_);
      }

      if (i == 0) {
         throw ERROR_GIVE_FAILED.create();
      }

      if (p_138208_.size() == 1) {
         p_138207_.sendSuccess(
            () -> Component.translatable("commands.recipe.give.success.single", p_138209_.size(), p_138208_.iterator().next().getDisplayName()), true
         );
      } else {
         p_138207_.sendSuccess(() -> Component.translatable("commands.recipe.give.success.multiple", p_138209_.size(), p_138208_.size()), true);
      }

      return i;
   }

   private static int takeRecipes(CommandSourceStack p_138213_, Collection<ServerPlayer> p_138214_, Collection<RecipeHolder<?>> p_138215_) throws CommandSyntaxException {
      int i = 0;

      for (ServerPlayer serverplayer : p_138214_) {
         i += serverplayer.resetRecipes(p_138215_);
      }

      if (i == 0) {
         throw ERROR_TAKE_FAILED.create();
      }

      if (p_138214_.size() == 1) {
         p_138213_.sendSuccess(
            () -> Component.translatable("commands.recipe.take.success.single", p_138215_.size(), p_138214_.iterator().next().getDisplayName()), true
         );
      } else {
         p_138213_.sendSuccess(() -> Component.translatable("commands.recipe.take.success.multiple", p_138215_.size(), p_138214_.size()), true);
      }

      return i;
   }
}
