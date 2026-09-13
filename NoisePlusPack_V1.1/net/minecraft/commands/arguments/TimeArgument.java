package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class TimeArgument implements ArgumentType<Integer> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");
   private static final SimpleCommandExceptionType ERROR_INVALID_UNIT = new SimpleCommandExceptionType(Component.translatable("argument.time.invalid_unit"));
   private static final Dynamic2CommandExceptionType ERROR_TICK_COUNT_TOO_LOW = new Dynamic2CommandExceptionType(
      (p_308389_, p_308390_) -> Component.translatableEscape("argument.time.tick_count_too_low", p_308390_, p_308389_)
   );
   private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap();
   final int minimum;

   private TimeArgument(int p_265107_) {
      this.minimum = p_265107_;
   }

   public static TimeArgument time() {
      return new TimeArgument(0);
   }

   public static TimeArgument time(int p_265722_) {
      return new TimeArgument(p_265722_);
   }

   public Integer parse(StringReader p_113039_) throws CommandSyntaxException {
      float f = p_113039_.readFloat();
      String s = p_113039_.readUnquotedString();
      int i = UNITS.getOrDefault(s, 0);
      if (i == 0) {
         throw ERROR_INVALID_UNIT.createWithContext(p_113039_);
      } else {
         int j = Math.round(f * i);
         if (j < this.minimum) {
            throw ERROR_TICK_COUNT_TOO_LOW.createWithContext(p_113039_, j, this.minimum);
         } else {
            return j;
         }
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_113044_, SuggestionsBuilder p_113045_) {
      StringReader stringreader = new StringReader(p_113045_.getRemaining());

      try {
         stringreader.readFloat();
      } catch (CommandSyntaxException commandsyntaxexception) {
         return p_113045_.buildFuture();
      }

      return SharedSuggestionProvider.suggest(UNITS.keySet(), p_113045_.createOffset(p_113045_.getStart() + stringreader.getCursor()));
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   static {
      UNITS.put("d", 24000);
      UNITS.put("s", 20);
      UNITS.put("t", 1);
      UNITS.put("", 1);
   }

   public static class Info implements ArgumentTypeInfo<TimeArgument, TimeArgument.Info.Template> {
      public void serializeToNetwork(TimeArgument.Info.Template p_265434_, FriendlyByteBuf p_265320_) {
         p_265320_.writeInt(p_265434_.min);
      }

      public TimeArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf p_265324_) {
         int i = p_265324_.readInt();
         return new TimeArgument.Info.Template(i);
      }

      public void serializeToJson(TimeArgument.Info.Template p_265110_, JsonObject p_265629_) {
         p_265629_.addProperty("min", p_265110_.min);
      }

      public TimeArgument.Info.Template unpack(TimeArgument p_265544_) {
         return new TimeArgument.Info.Template(p_265544_.minimum);
      }

      public final class Template implements ArgumentTypeInfo.Template<TimeArgument> {
         final int min;

         Template(final int p_265096_) {
            this.min = p_265096_;
         }

         public TimeArgument instantiate(CommandBuildContext p_265466_) {
            return TimeArgument.time(this.min);
         }

         @Override
         public ArgumentTypeInfo<TimeArgument, ?> type() {
            return Info.this;
         }
      }
   }
}
