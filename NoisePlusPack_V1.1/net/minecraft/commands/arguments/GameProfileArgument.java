package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;

public class GameProfileArgument implements ArgumentType<GameProfileArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
   public static final SimpleCommandExceptionType ERROR_UNKNOWN_PLAYER = new SimpleCommandExceptionType(Component.translatable("argument.player.unknown"));

   public static Collection<NameAndId> getGameProfiles(CommandContext<CommandSourceStack> p_94591_, String p_94592_) throws CommandSyntaxException {
      return ((GameProfileArgument.Result)p_94591_.getArgument(p_94592_, GameProfileArgument.Result.class)).getNames((CommandSourceStack)p_94591_.getSource());
   }

   public static GameProfileArgument gameProfile() {
      return new GameProfileArgument();
   }

   public <S> GameProfileArgument.Result parse(StringReader p_345547_, S p_345566_) throws CommandSyntaxException {
      return parse(p_345547_, EntitySelectorParser.allowSelectors(p_345566_));
   }

   public GameProfileArgument.Result parse(StringReader p_94586_) throws CommandSyntaxException {
      return parse(p_94586_, true);
   }

   private static GameProfileArgument.Result parse(StringReader p_345552_, boolean p_345567_) throws CommandSyntaxException {
      if (p_345552_.canRead() && p_345552_.peek() == '@') {
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(p_345552_, p_345567_);
         EntitySelector entityselector = entityselectorparser.parse();
         if (entityselector.includesEntities()) {
            throw EntityArgument.ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(p_345552_);
         } else {
            return new GameProfileArgument.SelectorResult(entityselector);
         }
      } else {
         int i = p_345552_.getCursor();

         while (p_345552_.canRead() && p_345552_.peek() != ' ') {
            p_345552_.skip();
         }

         String s = p_345552_.getString().substring(i, p_345552_.getCursor());
         return p_421089_ -> {
            Optional<NameAndId> optional = p_421089_.getServer().services().nameToIdCache().get(s);
            return Collections.singleton(optional.orElseThrow(ERROR_UNKNOWN_PLAYER::create));
         };
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_94598_, SuggestionsBuilder p_94599_) {
      if (p_94598_.getSource() instanceof SharedSuggestionProvider sharedsuggestionprovider) {
         StringReader stringreader = new StringReader(p_94599_.getInput());
         stringreader.setCursor(p_94599_.getStart());
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(
            stringreader, sharedsuggestionprovider.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)
         );

         try {
            entityselectorparser.parse();
         } catch (CommandSyntaxException var7) {
         }

         return entityselectorparser.fillSuggestions(
            p_94599_, p_345543_ -> SharedSuggestionProvider.suggest(sharedsuggestionprovider.getOnlinePlayerNames(), p_345543_)
         );
      } else {
         return Suggestions.empty();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   @FunctionalInterface
   public interface Result {
      Collection<NameAndId> getNames(CommandSourceStack var1) throws CommandSyntaxException;
   }

   public static class SelectorResult implements GameProfileArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(EntitySelector p_94605_) {
         this.selector = p_94605_;
      }

      @Override
      public Collection<NameAndId> getNames(CommandSourceStack p_94607_) throws CommandSyntaxException {
         List<ServerPlayer> list = this.selector.findPlayers(p_94607_);
         if (list.isEmpty()) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
         }

         List<NameAndId> list1 = new ArrayList<>();

         for (ServerPlayer serverplayer : list) {
            list1.add(serverplayer.nameAndId());
         }

         return list1;
      }
   }
}
