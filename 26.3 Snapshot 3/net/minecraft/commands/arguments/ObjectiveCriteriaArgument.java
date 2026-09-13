package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ObjectiveCriteriaArgument implements ArgumentType<ObjectiveCriteria> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar.baz", "minecraft:foo");
   public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("argument.criteria.invalid", value)
   );

   private ObjectiveCriteriaArgument() {
   }

   public static ObjectiveCriteriaArgument criteria() {
      return new ObjectiveCriteriaArgument();
   }

   public static ObjectiveCriteria getCriteria(final CommandContext<CommandSourceStack> context, final String name) {
      return (ObjectiveCriteria)context.getArgument(name, ObjectiveCriteria.class);
   }

   public ObjectiveCriteria parse(final StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();

      while (reader.canRead() && reader.peek() != ' ') {
         reader.skip();
      }

      String id = reader.getString().substring(start, reader.getCursor());
      return ObjectiveCriteria.byName(id).orElseThrow(() -> {
         reader.setCursor(start);
         return ERROR_INVALID_VALUE.createWithContext(reader, id);
      });
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      List<String> ids = Lists.newArrayList(ObjectiveCriteria.getCustomCriteriaNames());

      for (StatType<?> type : BuiltInRegistries.STAT_TYPE) {
         for (Object value : type.getRegistry()) {
            String name = this.getName(type, value);
            ids.add(name);
         }
      }

      return SharedSuggestionProvider.suggest(ids, builder);
   }

   public <T> String getName(final StatType<T> type, final Object value) {
      return Stat.buildName(type, (T)value);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
