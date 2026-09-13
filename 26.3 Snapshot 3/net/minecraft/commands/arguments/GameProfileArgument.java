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

   public static Collection<NameAndId> getGameProfiles(final CommandContext<CommandSourceStack> source, final String name) throws CommandSyntaxException {
      return ((GameProfileArgument.Result)source.getArgument(name, GameProfileArgument.Result.class)).getNames((CommandSourceStack)source.getSource());
   }

   public static GameProfileArgument gameProfile() {
      return new GameProfileArgument();
   }

   public <S> GameProfileArgument.Result parse(final StringReader reader, final S source) throws CommandSyntaxException {
      return parse(reader, EntitySelectorParser.allowSelectors(source));
   }

   public GameProfileArgument.Result parse(final StringReader reader) throws CommandSyntaxException {
      return parse(reader, true);
   }

   private static GameProfileArgument.Result parse(final StringReader reader, final boolean allowSelectors) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '@') {
         EntitySelectorParser parser = new EntitySelectorParser(reader, allowSelectors);
         EntitySelector parse = parser.parse();
         if (parse.includesEntities()) {
            throw EntityArgument.ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(reader);
         } else {
            return new GameProfileArgument.SelectorResult(parse);
         }
      } else {
         int start = reader.getCursor();

         while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
         }

         String name = reader.getString().substring(start, reader.getCursor());
         return c -> {
            Optional<NameAndId> result = c.getServer().services().nameToIdCache().get(name);
            return Collections.singleton(result.orElseThrow(ERROR_UNKNOWN_PLAYER::create));
         };
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> contextBuilder, final SuggestionsBuilder builder) {
      if (contextBuilder.getSource() instanceof SharedSuggestionProvider source) {
         StringReader reader = new StringReader(builder.getInput());
         reader.setCursor(builder.getStart());
         EntitySelectorParser parser = new EntitySelectorParser(reader, source.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS));

         try {
            parser.parse();
         } catch (CommandSyntaxException var7) {
         }

         return parser.fillSuggestions(builder, suggestions -> SharedSuggestionProvider.suggest(source.getOnlinePlayerNames(), suggestions));
      } else {
         return Suggestions.empty();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   @FunctionalInterface
   public interface Result {
      Collection<NameAndId> getNames(final CommandSourceStack sender) throws CommandSyntaxException;
   }

   public static class SelectorResult implements GameProfileArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(final EntitySelector selector) {
         this.selector = selector;
      }

      @Override
      public Collection<NameAndId> getNames(final CommandSourceStack sender) throws CommandSyntaxException {
         List<ServerPlayer> players = this.selector.findPlayers(sender);
         if (players.isEmpty()) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
         }

         List<NameAndId> result = new ArrayList<>();

         for (ServerPlayer entity : players) {
            result.add(entity.nameAndId());
         }

         return result;
      }
   }
}
