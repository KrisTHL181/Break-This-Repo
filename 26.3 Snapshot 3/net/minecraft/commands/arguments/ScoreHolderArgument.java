package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.ScoreHolder;

public class ScoreHolderArgument implements ArgumentType<ScoreHolderArgument.Result> {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_SCORE_HOLDERS = (context, builder) -> {
      StringReader reader = new StringReader(builder.getInput());
      reader.setCursor(builder.getStart());
      EntitySelectorParser parser = new EntitySelectorParser(
         reader, ((CommandSourceStack)context.getSource()).permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)
      );

      try {
         parser.parse();
      } catch (CommandSyntaxException var5) {
      }

      return parser.fillSuggestions(
         builder, suggestions -> SharedSuggestionProvider.suggest(((CommandSourceStack)context.getSource()).getOnlinePlayerNames(), suggestions)
      );
   };
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
   private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(Component.translatable("argument.scoreHolder.empty"));
   private final boolean multiple;

   public ScoreHolderArgument(final boolean multiple) {
      this.multiple = multiple;
   }

   public static ScoreHolder getName(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return getNames(context, name).iterator().next();
   }

   public static Collection<ScoreHolder> getNames(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return getNames(context, name, Collections::emptyList);
   }

   public static Collection<ScoreHolder> getNamesWithDefaultWildcard(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return getNames(context, name, ((CommandSourceStack)context.getSource()).getServer().getScoreboard()::getTrackedPlayers);
   }

   public static Collection<ScoreHolder> getNames(
      final CommandContext<CommandSourceStack> context, final String name, final Supplier<Collection<ScoreHolder>> wildcard
   ) throws CommandSyntaxException {
      Collection<ScoreHolder> result = ((ScoreHolderArgument.Result)context.getArgument(name, ScoreHolderArgument.Result.class))
         .getNames((CommandSourceStack)context.getSource(), wildcard);
      if (result.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else {
         return result;
      }
   }

   public static ScoreHolderArgument scoreHolder() {
      return new ScoreHolderArgument(false);
   }

   public static ScoreHolderArgument scoreHolders() {
      return new ScoreHolderArgument(true);
   }

   public ScoreHolderArgument.Result parse(final StringReader reader) throws CommandSyntaxException {
      return this.parse(reader, true);
   }

   public <S> ScoreHolderArgument.Result parse(final StringReader reader, final S source) throws CommandSyntaxException {
      return this.parse(reader, EntitySelectorParser.allowSelectors(source));
   }

   private ScoreHolderArgument.Result parse(final StringReader reader, final boolean allowSelectors) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '@') {
         EntitySelectorParser parser = new EntitySelectorParser(reader, allowSelectors);
         EntitySelector selector = parser.parse();
         if (!this.multiple && selector.getMaxResults() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.createWithContext(reader);
         } else {
            return new ScoreHolderArgument.SelectorResult(selector);
         }
      } else {
         int start = reader.getCursor();

         while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
         }

         String text = reader.getString().substring(start, reader.getCursor());
         if (text.equals("*")) {
            return (sender, wildcard) -> {
               Collection<ScoreHolder> results = wildcard.get();
               if (results.isEmpty()) {
                  throw ERROR_NO_RESULTS.create();
               } else {
                  return results;
               }
            };
         }

         List<ScoreHolder> nameOnlyHolder = List.of(ScoreHolder.forNameOnly(text));
         if (text.startsWith("#")) {
            return (sender, wildcard) -> nameOnlyHolder;
         }

         try {
            UUID uuid = UUID.fromString(text);
            return (sender, wildcard) -> {
               MinecraftServer server = sender.getServer();
               ScoreHolder firstResult = null;
               List<ScoreHolder> moreResults = null;

               for (ServerLevel level : server.getAllLevels()) {
                  Entity entity = level.getEntity(uuid);
                  if (entity != null) {
                     if (firstResult == null) {
                        firstResult = entity;
                     } else {
                        if (moreResults == null) {
                           moreResults = new ArrayList<>();
                           moreResults.add(firstResult);
                        }

                        moreResults.add(entity);
                     }
                  }
               }

               if (moreResults != null) {
                  return moreResults;
               } else {
                  return firstResult != null ? List.of(firstResult) : nameOnlyHolder;
               }
            };
         } catch (IllegalArgumentException var7) {
            return (sender, wildcard) -> {
               MinecraftServer server = sender.getServer();
               ServerPlayer player = server.getPlayerList().getPlayerByName(text);
               return player != null ? List.of(player) : nameOnlyHolder;
            };
         }
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info implements ArgumentTypeInfo<ScoreHolderArgument, ScoreHolderArgument.Info.Template> {
      private static final byte FLAG_MULTIPLE = 1;

      public void serializeToNetwork(final ScoreHolderArgument.Info.Template template, final FriendlyByteBuf out) {
         int flags = 0;
         if (template.multiple) {
            flags |= 1;
         }

         out.writeByte(flags);
      }

      public ScoreHolderArgument.Info.Template deserializeFromNetwork(final FriendlyByteBuf in) {
         byte flags = in.readByte();
         boolean multiple = (flags & 1) != 0;
         return new ScoreHolderArgument.Info.Template(multiple);
      }

      public void serializeToJson(final ScoreHolderArgument.Info.Template template, final JsonObject out) {
         out.addProperty("amount", template.multiple ? "multiple" : "single");
      }

      public ScoreHolderArgument.Info.Template unpack(final ScoreHolderArgument argument) {
         return new ScoreHolderArgument.Info.Template(argument.multiple);
      }

      public final class Template implements ArgumentTypeInfo.Template<ScoreHolderArgument> {
         private final boolean multiple;

         private Template(final boolean multiple) {
            this.multiple = multiple;
         }

         public ScoreHolderArgument instantiate(final CommandBuildContext context) {
            return new ScoreHolderArgument(this.multiple);
         }

         @Override
         public ArgumentTypeInfo<ScoreHolderArgument, ?> type() {
            return Info.this;
         }
      }
   }

   @FunctionalInterface
   public interface Result {
      Collection<ScoreHolder> getNames(final CommandSourceStack sender, Supplier<Collection<ScoreHolder>> wildcard) throws CommandSyntaxException;
   }

   public static class SelectorResult implements ScoreHolderArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(final EntitySelector selector) {
         this.selector = selector;
      }

      @Override
      public Collection<ScoreHolder> getNames(final CommandSourceStack sender, final Supplier<Collection<ScoreHolder>> wildcard) throws CommandSyntaxException {
         List<? extends Entity> entities = this.selector.findEntities(sender);
         if (entities.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
         } else {
            return List.copyOf(entities);
         }
      }
   }
}
