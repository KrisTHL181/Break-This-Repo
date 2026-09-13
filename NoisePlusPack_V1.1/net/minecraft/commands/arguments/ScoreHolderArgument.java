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
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_SCORE_HOLDERS = (p_448505_, p_448506_) -> {
      StringReader stringreader = new StringReader(p_448506_.getInput());
      stringreader.setCursor(p_448506_.getStart());
      EntitySelectorParser entityselectorparser = new EntitySelectorParser(
         stringreader, ((CommandSourceStack)p_448505_.getSource()).permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)
      );

      try {
         entityselectorparser.parse();
      } catch (CommandSyntaxException var5) {
      }

      return entityselectorparser.fillSuggestions(
         p_448506_, p_171606_ -> SharedSuggestionProvider.suggest(((CommandSourceStack)p_448505_.getSource()).getOnlinePlayerNames(), p_171606_)
      );
   };
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
   private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(Component.translatable("argument.scoreHolder.empty"));
   final boolean multiple;

   public ScoreHolderArgument(boolean p_108216_) {
      this.multiple = p_108216_;
   }

   public static ScoreHolder getName(CommandContext<CommandSourceStack> p_108224_, String p_108225_) throws CommandSyntaxException {
      return getNames(p_108224_, p_108225_).iterator().next();
   }

   public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> p_108244_, String p_108245_) throws CommandSyntaxException {
      return getNames(p_108244_, p_108245_, Collections::emptyList);
   }

   public static Collection<ScoreHolder> getNamesWithDefaultWildcard(CommandContext<CommandSourceStack> p_108247_, String p_108248_) throws CommandSyntaxException {
      return getNames(p_108247_, p_108248_, ((CommandSourceStack)p_108247_.getSource()).getServer().getScoreboard()::getTrackedPlayers);
   }

   public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> p_108227_, String p_108228_, Supplier<Collection<ScoreHolder>> p_108229_) throws CommandSyntaxException {
      Collection<ScoreHolder> collection = ((ScoreHolderArgument.Result)p_108227_.getArgument(p_108228_, ScoreHolderArgument.Result.class))
         .getNames((CommandSourceStack)p_108227_.getSource(), p_108229_);
      if (collection.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else {
         return collection;
      }
   }

   public static ScoreHolderArgument scoreHolder() {
      return new ScoreHolderArgument(false);
   }

   public static ScoreHolderArgument scoreHolders() {
      return new ScoreHolderArgument(true);
   }

   public ScoreHolderArgument.Result parse(StringReader p_108219_) throws CommandSyntaxException {
      return this.parse(p_108219_, true);
   }

   public <S> ScoreHolderArgument.Result parse(StringReader p_345560_, S p_345549_) throws CommandSyntaxException {
      return this.parse(p_345560_, EntitySelectorParser.allowSelectors(p_345549_));
   }

   private ScoreHolderArgument.Result parse(StringReader p_345561_, boolean p_345551_) throws CommandSyntaxException {
      if (p_345561_.canRead() && p_345561_.peek() == '@') {
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(p_345561_, p_345551_);
         EntitySelector entityselector = entityselectorparser.parse();
         if (!this.multiple && entityselector.getMaxResults() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.createWithContext(p_345561_);
         } else {
            return new ScoreHolderArgument.SelectorResult(entityselector);
         }
      } else {
         int i = p_345561_.getCursor();

         while (p_345561_.canRead() && p_345561_.peek() != ' ') {
            p_345561_.skip();
         }

         String s = p_345561_.getString().substring(i, p_345561_.getCursor());
         if (s.equals("*")) {
            return (p_108231_, p_108232_) -> {
               Collection<ScoreHolder> collection = p_108232_.get();
               if (collection.isEmpty()) {
                  throw ERROR_NO_RESULTS.create();
               } else {
                  return collection;
               }
            };
         }

         List<ScoreHolder> list = List.of(ScoreHolder.forNameOnly(s));
         if (s.startsWith("#")) {
            return (p_108237_, p_108238_) -> list;
         }

         try {
            UUID uuid = UUID.fromString(s);
            return (p_308383_, p_308384_) -> {
               MinecraftServer minecraftserver = p_308383_.getServer();
               ScoreHolder scoreholder = null;
               List<ScoreHolder> list1 = null;

               for (ServerLevel serverlevel : minecraftserver.getAllLevels()) {
                  Entity entity = serverlevel.getEntity(uuid);
                  if (entity != null) {
                     if (scoreholder == null) {
                        scoreholder = entity;
                     } else {
                        if (list1 == null) {
                           list1 = new ArrayList<>();
                           list1.add(scoreholder);
                        }

                        list1.add(entity);
                     }
                  }
               }

               if (list1 != null) {
                  return list1;
               } else {
                  return scoreholder != null ? List.of(scoreholder) : list;
               }
            };
         } catch (IllegalArgumentException illegalargumentexception) {
            return (p_308379_, p_308380_) -> {
               MinecraftServer minecraftserver = p_308379_.getServer();
               ServerPlayer serverplayer = minecraftserver.getPlayerList().getPlayerByName(s);
               return serverplayer != null ? List.of(serverplayer) : list;
            };
         }
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info implements ArgumentTypeInfo<ScoreHolderArgument, ScoreHolderArgument.Info.Template> {
      private static final byte FLAG_MULTIPLE = 1;

      public void serializeToNetwork(ScoreHolderArgument.Info.Template p_233469_, FriendlyByteBuf p_233470_) {
         int i = 0;
         if (p_233469_.multiple) {
            i |= 1;
         }

         p_233470_.writeByte(i);
      }

      public ScoreHolderArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf p_233480_) {
         byte b0 = p_233480_.readByte();
         boolean flag = (b0 & 1) != 0;
         return new ScoreHolderArgument.Info.Template(flag);
      }

      public void serializeToJson(ScoreHolderArgument.Info.Template p_233466_, JsonObject p_233467_) {
         p_233467_.addProperty("amount", p_233466_.multiple ? "multiple" : "single");
      }

      public ScoreHolderArgument.Info.Template unpack(ScoreHolderArgument p_233472_) {
         return new ScoreHolderArgument.Info.Template(p_233472_.multiple);
      }

      public final class Template implements ArgumentTypeInfo.Template<ScoreHolderArgument> {
         final boolean multiple;

         Template(final boolean p_233487_) {
            this.multiple = p_233487_;
         }

         public ScoreHolderArgument instantiate(CommandBuildContext p_233490_) {
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
      Collection<ScoreHolder> getNames(CommandSourceStack var1, Supplier<Collection<ScoreHolder>> var2) throws CommandSyntaxException;
   }

   public static class SelectorResult implements ScoreHolderArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(EntitySelector p_108256_) {
         this.selector = p_108256_;
      }

      @Override
      public Collection<ScoreHolder> getNames(CommandSourceStack p_108258_, Supplier<Collection<ScoreHolder>> p_108259_) throws CommandSyntaxException {
         List<? extends Entity> list = this.selector.findEntities(p_108258_);
         if (list.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
         } else {
            return List.copyOf(list);
         }
      }
   }
}
