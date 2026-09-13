package net.minecraft.commands.arguments;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;

public class EntityArgument implements ArgumentType<EntitySelector> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
   public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_ENTITY = new SimpleCommandExceptionType(Component.translatable("argument.entity.toomany"));
   public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER = new SimpleCommandExceptionType(Component.translatable("argument.player.toomany"));
   public static final SimpleCommandExceptionType ERROR_ONLY_PLAYERS_ALLOWED = new SimpleCommandExceptionType(
      Component.translatable("argument.player.entities")
   );
   public static final SimpleCommandExceptionType NO_ENTITIES_FOUND = new SimpleCommandExceptionType(Component.translatable("argument.entity.notfound.entity"));
   public static final SimpleCommandExceptionType NO_PLAYERS_FOUND = new SimpleCommandExceptionType(Component.translatable("argument.entity.notfound.player"));
   public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.selector.not_allowed")
   );
   private final boolean single;
   private final boolean playersOnly;

   protected EntityArgument(final boolean single, final boolean playersOnly) {
      this.single = single;
      this.playersOnly = playersOnly;
   }

   public static EntityArgument entity() {
      return new EntityArgument(true, false);
   }

   public static Entity getEntity(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return ((EntitySelector)context.getArgument(name, EntitySelector.class)).findSingleEntity((CommandSourceStack)context.getSource());
   }

   public static EntityArgument entities() {
      return new EntityArgument(false, false);
   }

   public static Collection<? extends Entity> getEntities(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      Collection<? extends Entity> result = getOptionalEntities(context, name);
      if (result.isEmpty()) {
         throw NO_ENTITIES_FOUND.create();
      } else {
         return result;
      }
   }

   public static Collection<? extends Entity> getOptionalEntities(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return ((EntitySelector)context.getArgument(name, EntitySelector.class)).findEntities((CommandSourceStack)context.getSource());
   }

   public static Collection<ServerPlayer> getOptionalPlayers(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return ((EntitySelector)context.getArgument(name, EntitySelector.class)).findPlayers((CommandSourceStack)context.getSource());
   }

   public static EntityArgument player() {
      return new EntityArgument(true, true);
   }

   public static ServerPlayer getPlayer(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return ((EntitySelector)context.getArgument(name, EntitySelector.class)).findSinglePlayer((CommandSourceStack)context.getSource());
   }

   public static EntityArgument players() {
      return new EntityArgument(false, true);
   }

   public static Collection<ServerPlayer> getPlayers(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      List<ServerPlayer> players = ((EntitySelector)context.getArgument(name, EntitySelector.class)).findPlayers((CommandSourceStack)context.getSource());
      if (players.isEmpty()) {
         throw NO_PLAYERS_FOUND.create();
      } else {
         return players;
      }
   }

   public EntitySelector parse(final StringReader reader) throws CommandSyntaxException {
      return this.parse(reader, true);
   }

   public <S> EntitySelector parse(final StringReader reader, final S source) throws CommandSyntaxException {
      return this.parse(reader, EntitySelectorParser.allowSelectors(source));
   }

   private EntitySelector parse(final StringReader reader, final boolean allowSelectors) throws CommandSyntaxException {
      int start = 0;
      EntitySelectorParser parser = new EntitySelectorParser(reader, allowSelectors);
      EntitySelector selector = parser.parse();
      if (selector.getMaxResults() > 1 && this.single) {
         if (this.playersOnly) {
            reader.setCursor(0);
            throw ERROR_NOT_SINGLE_PLAYER.createWithContext(reader);
         } else {
            reader.setCursor(0);
            throw ERROR_NOT_SINGLE_ENTITY.createWithContext(reader);
         }
      } else if (selector.includesEntities() && this.playersOnly && !selector.isSelfSelector()) {
         reader.setCursor(0);
         throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(reader);
      } else {
         return selector;
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

         return parser.fillSuggestions(builder, suggestions -> {
            Collection<String> onlinePlayerNames = source.getOnlinePlayerNames();
            Iterable<String> suggestedNames = this.playersOnly ? onlinePlayerNames : Iterables.concat(onlinePlayerNames, source.getSelectedEntities());
            SharedSuggestionProvider.suggest(suggestedNames, suggestions);
         });
      } else {
         return Suggestions.empty();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info implements ArgumentTypeInfo<EntityArgument, EntityArgument.Info.Template> {
      private static final byte FLAG_SINGLE = 1;
      private static final byte FLAG_PLAYERS_ONLY = 2;

      public void serializeToNetwork(final EntityArgument.Info.Template template, final FriendlyByteBuf out) {
         int flags = 0;
         if (template.single) {
            flags |= 1;
         }

         if (template.playersOnly) {
            flags |= 2;
         }

         out.writeByte(flags);
      }

      public EntityArgument.Info.Template deserializeFromNetwork(final FriendlyByteBuf in) {
         byte flags = in.readByte();
         return new EntityArgument.Info.Template((flags & 1) != 0, (flags & 2) != 0);
      }

      public void serializeToJson(final EntityArgument.Info.Template template, final JsonObject out) {
         out.addProperty("amount", template.single ? "single" : "multiple");
         out.addProperty("type", template.playersOnly ? "players" : "entities");
      }

      public EntityArgument.Info.Template unpack(final EntityArgument argument) {
         return new EntityArgument.Info.Template(argument.single, argument.playersOnly);
      }

      public final class Template implements ArgumentTypeInfo.Template<EntityArgument> {
         private final boolean single;
         private final boolean playersOnly;

         private Template(final boolean single, final boolean playersOnly) {
            this.single = single;
            this.playersOnly = playersOnly;
         }

         public EntityArgument instantiate(final CommandBuildContext context) {
            return new EntityArgument(this.single, this.playersOnly);
         }

         @Override
         public ArgumentTypeInfo<EntityArgument, ?> type() {
            return Info.this;
         }
      }
   }
}
