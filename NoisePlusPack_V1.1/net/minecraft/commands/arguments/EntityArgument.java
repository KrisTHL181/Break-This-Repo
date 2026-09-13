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
   final boolean single;
   final boolean playersOnly;

   protected EntityArgument(boolean p_91447_, boolean p_91448_) {
      this.single = p_91447_;
      this.playersOnly = p_91448_;
   }

   public static EntityArgument entity() {
      return new EntityArgument(true, false);
   }

   public static Entity getEntity(CommandContext<CommandSourceStack> p_91453_, String p_91454_) throws CommandSyntaxException {
      return ((EntitySelector)p_91453_.getArgument(p_91454_, EntitySelector.class)).findSingleEntity((CommandSourceStack)p_91453_.getSource());
   }

   public static EntityArgument entities() {
      return new EntityArgument(false, false);
   }

   public static Collection<? extends Entity> getEntities(CommandContext<CommandSourceStack> p_91462_, String p_91463_) throws CommandSyntaxException {
      Collection<? extends Entity> collection = getOptionalEntities(p_91462_, p_91463_);
      if (collection.isEmpty()) {
         throw NO_ENTITIES_FOUND.create();
      } else {
         return collection;
      }
   }

   public static Collection<? extends Entity> getOptionalEntities(CommandContext<CommandSourceStack> p_91468_, String p_91469_) throws CommandSyntaxException {
      return ((EntitySelector)p_91468_.getArgument(p_91469_, EntitySelector.class)).findEntities((CommandSourceStack)p_91468_.getSource());
   }

   public static Collection<ServerPlayer> getOptionalPlayers(CommandContext<CommandSourceStack> p_91472_, String p_91473_) throws CommandSyntaxException {
      return ((EntitySelector)p_91472_.getArgument(p_91473_, EntitySelector.class)).findPlayers((CommandSourceStack)p_91472_.getSource());
   }

   public static EntityArgument player() {
      return new EntityArgument(true, true);
   }

   public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> p_91475_, String p_91476_) throws CommandSyntaxException {
      return ((EntitySelector)p_91475_.getArgument(p_91476_, EntitySelector.class)).findSinglePlayer((CommandSourceStack)p_91475_.getSource());
   }

   public static EntityArgument players() {
      return new EntityArgument(false, true);
   }

   public static Collection<ServerPlayer> getPlayers(CommandContext<CommandSourceStack> p_91478_, String p_91479_) throws CommandSyntaxException {
      List<ServerPlayer> list = ((EntitySelector)p_91478_.getArgument(p_91479_, EntitySelector.class)).findPlayers((CommandSourceStack)p_91478_.getSource());
      if (list.isEmpty()) {
         throw NO_PLAYERS_FOUND.create();
      } else {
         return list;
      }
   }

   public EntitySelector parse(StringReader p_91451_) throws CommandSyntaxException {
      return this.parse(p_91451_, true);
   }

   public <S> EntitySelector parse(StringReader p_345548_, S p_345559_) throws CommandSyntaxException {
      return this.parse(p_345548_, EntitySelectorParser.allowSelectors(p_345559_));
   }

   private EntitySelector parse(StringReader p_345565_, boolean p_345553_) throws CommandSyntaxException {
      int i = 0;
      EntitySelectorParser entityselectorparser = new EntitySelectorParser(p_345565_, p_345553_);
      EntitySelector entityselector = entityselectorparser.parse();
      if (entityselector.getMaxResults() > 1 && this.single) {
         if (this.playersOnly) {
            p_345565_.setCursor(0);
            throw ERROR_NOT_SINGLE_PLAYER.createWithContext(p_345565_);
         } else {
            p_345565_.setCursor(0);
            throw ERROR_NOT_SINGLE_ENTITY.createWithContext(p_345565_);
         }
      } else if (entityselector.includesEntities() && this.playersOnly && !entityselector.isSelfSelector()) {
         p_345565_.setCursor(0);
         throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(p_345565_);
      } else {
         return entityselector;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_91482_, SuggestionsBuilder p_91483_) {
      if (p_91482_.getSource() instanceof SharedSuggestionProvider sharedsuggestionprovider) {
         StringReader stringreader = new StringReader(p_91483_.getInput());
         stringreader.setCursor(p_91483_.getStart());
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(
            stringreader, sharedsuggestionprovider.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)
         );

         try {
            entityselectorparser.parse();
         } catch (CommandSyntaxException var7) {
         }

         return entityselectorparser.fillSuggestions(p_91483_, p_91457_ -> {
            Collection<String> collection = sharedsuggestionprovider.getOnlinePlayerNames();
            Iterable<String> iterable = this.playersOnly ? collection : Iterables.concat(collection, sharedsuggestionprovider.getSelectedEntities());
            SharedSuggestionProvider.suggest(iterable, p_91457_);
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

      public void serializeToNetwork(EntityArgument.Info.Template p_231271_, FriendlyByteBuf p_231272_) {
         int i = 0;
         if (p_231271_.single) {
            i |= 1;
         }

         if (p_231271_.playersOnly) {
            i |= 2;
         }

         p_231272_.writeByte(i);
      }

      public EntityArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf p_231282_) {
         byte b0 = p_231282_.readByte();
         return new EntityArgument.Info.Template((b0 & 1) != 0, (b0 & 2) != 0);
      }

      public void serializeToJson(EntityArgument.Info.Template p_231268_, JsonObject p_231269_) {
         p_231269_.addProperty("amount", p_231268_.single ? "single" : "multiple");
         p_231269_.addProperty("type", p_231268_.playersOnly ? "players" : "entities");
      }

      public EntityArgument.Info.Template unpack(EntityArgument p_231274_) {
         return new EntityArgument.Info.Template(p_231274_.single, p_231274_.playersOnly);
      }

      public final class Template implements ArgumentTypeInfo.Template<EntityArgument> {
         final boolean single;
         final boolean playersOnly;

         Template(final boolean p_231290_, final boolean p_231291_) {
            this.single = p_231290_;
            this.playersOnly = p_231291_;
         }

         public EntityArgument instantiate(CommandBuildContext p_231294_) {
            return new EntityArgument(this.single, this.playersOnly);
         }

         @Override
         public ArgumentTypeInfo<EntityArgument, ?> type() {
            return Info.this;
         }
      }
   }
}
